package com.makkii.maic.oldAI.newAI;

import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Transformer {

    public static final String TRAINED_MODEL_FILE = "trained_model.json";
    public static final int NUM_LAYERS = 6;
    public static final int NUM_HEADS = 8;
    public static final int D_MODEL = 512;
    public static final int D_FF = 2048; // Feed Forward Networkの中間層の次元
    public static final double DROPOUT_RATE = 0.1; // ドロップアウト率 (必要に応じて調整)
    public static final double LEARNING_RATE = 0.001; // 学習率 (Adamのデフォルト値を使用)
    public static final int BATCH_SIZE = 32;

    private final String configDir;
    private final BPE bpe;
    private final Word2Vec word2Vec;

    // モデルパラメータ
    private final Map<String, double[][]> weights;
    private final Map<String, double[]> biases;
    //private double[] positionalEncodings;

    public Transformer(String configDir) throws IOException {
        this.configDir = configDir;
        this.bpe = new BPE(configDir);
        this.word2Vec = new Word2Vec(configDir);
        this.weights = new HashMap<>();
        this.biases = new HashMap<>();
        //this.positionalEncodings = new double[MAX_SEQUENCE_LENGTH];
        //initPositionalEncoding();
        loadOrTrainModel(); //モデル読み込み
    }

    //学習済みモデルの読み込み、学習
    public void loadOrTrainModel() throws IOException {
        File modelFile = new File(configDir, TRAINED_MODEL_FILE);
        if (modelFile.exists()) {
            loadModel(modelFile);
        } else {
            //パラメータ初期化
            initializeParameters();
            //trainModel();
            saveModel(modelFile);
        }
    }

    // パラメータの初期化
    private void initializeParameters() {
        Random random = new Random();
        // Decoder層のパラメータ
        for (int i = 0; i < NUM_LAYERS; i++) {
            // Masked Multi-Head Self-Attention
            weights.put("decoder_" + i + "_mha_q", initializeMatrix(D_MODEL, D_MODEL, random));
            weights.put("decoder_" + i + "_mha_k", initializeMatrix(D_MODEL, D_MODEL, random));
            weights.put("decoder_" + i + "_mha_v", initializeMatrix(D_MODEL, D_MODEL, random));
            weights.put("decoder_" + i + "_mha_o", initializeMatrix(D_MODEL, D_MODEL, random));

            // Feed Forward Network
            weights.put("decoder_" + i + "_ffn_1", initializeMatrix(D_MODEL, D_FF, random));
            biases.put("decoder_" + i + "_ffn_1", initializeArray(D_FF, random));
            weights.put("decoder_" + i + "_ffn_2", initializeMatrix(D_FF, D_MODEL, random));
            biases.put("decoder_" + i + "_ffn_2", initializeArray(D_MODEL, random));
        }

        // Linear層
        weights.put("linear", initializeMatrix(D_MODEL, bpe.tokenize("").size(), random));
        biases.put("linear", initializeArray(bpe.tokenize("").size(), random));
    }

    //行列の初期化
    private double[][] initializeMatrix(int rows, int cols, Random random) {
        double[][] matrix = new double[rows][cols];
        double stddev = Math.sqrt(2.0 / (rows + cols)); // Xavierの初期化
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextGaussian() * stddev;
            }
        }
        return matrix;
    }

    //配列の初期化
    private double[] initializeArray(int size, Random random) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = 0; // バイアスは0で初期化
        }
        return array;
    }

    // 位置エンコーディングの初期化
    private double[] positionalEncoding(int position) {
        double[] pe = new double[D_MODEL];
        for (int i = 0; i < D_MODEL; i += 2) {
            pe[i] = Math.sin(position / Math.pow(10000, (2.0 * i) / D_MODEL));
            pe[i + 1] = Math.cos(position / Math.pow(10000, (2.0 * i) / D_MODEL));
        }
        return pe;
    }

    // Masked Multi-Head Attention
    private double[][][] maskedMultiHeadAttention(double[][][] input, int layer) {

        // Q, K, Vの計算
        double[][][] q = matrixMultiply(input, weights.get("decoder_" + layer + "_mha_q"));
        double[][][] k = matrixMultiply(input, weights.get("decoder_" + layer + "_mha_k"));
        double[][][] v = matrixMultiply(input, weights.get("decoder_" + layer + "_mha_v"));

        // ヘッドに分割
        double[][][][] qSplit = splitHeads(q, NUM_HEADS);
        double[][][][] kSplit = splitHeads(k, NUM_HEADS);
        double[][][][] vSplit = splitHeads(v, NUM_HEADS);

        // Scaled Dot-Product Attention
        double[][][][] attention = scaledDotProductAttention(qSplit, kSplit, vSplit, true);

        // ヘッドの結合
        double[][][] concatAttention = concatenateHeads(attention);

        // 行列乗算のために形状を変換
        //double[][] reshapedMatrix = reshapeForMatrixMultiply(concatAttention);

        // 線形層
        double[][][] output = matrixMultiply(concatAttention, weights.get("decoder_" + layer + "_mha_o"));

        return output;
    }

    private double[][][] matrixMultiply(double[][][] a, double[][] b) {
        int batchSize = a.length;
        int seqLen = a[0].length;
        int aCols = a[0][0].length; // a の dModel (入力次元)
        int bRows = b.length;
        int bCols = b[0].length;

        if (aCols != bRows) {
            throw new IllegalArgumentException("Matrix dimensions are not compatible for multiplication: " + aCols + " != " + bRows);
        }

        double[][][] result = new double[batchSize][seqLen][bCols];
        for (int batch = 0; batch < batchSize; batch++) {
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < bCols; j++) {
                    for (int k = 0; k < aCols; k++) {
                        result[batch][i][j] += a[batch][i][k] * b[k][j];
                    }
                }
            }
        }
        return result;
    }

    private double[][] reshapeForMatrixMultiply(double[][][] input) {
        int batchSize = input.length;
        int seqLen = input[0].length;
        int dModel = input[0][0].length;

        // バッチサイズ x 系列長, モデルの次元数 の2次元配列に変換
        double[][] reshaped = new double[batchSize * seqLen][dModel];

        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                System.arraycopy(input[b][i], 0, reshaped[b * seqLen + i], 0, dModel);
            }
        }
        return reshaped;
    }

    // Feed Forward Network
    private double[][][] feedForwardNetwork(double[][][] input, int layer) {
        // 第1層
        double[][][] linear1 = matrixMultiply(input, weights.get("decoder_" + layer + "_ffn_1"));
        linear1 = addBias(linear1, biases.get("decoder_" + layer + "_ffn_1"));
        linear1 = relu(linear1);

        // 第2層
        double[][][] linear2 = matrixMultiply(linear1, weights.get("decoder_" + layer + "_ffn_2"));
        linear2 = addBias(linear2, biases.get("decoder_" + layer + "_ffn_2"));

        return linear2;
    }

    // Scaled Dot-Product Attention
    private double[][][][] scaledDotProductAttention(double[][][][] q, double[][][][] k, double[][][][] v, boolean masked) {
        int batchSize = q.length;
        int seqLen = q[0].length;
        int headDim = q[0][0].length; // d_k

        double[][][][] output = new double[batchSize][NUM_HEADS][seqLen][headDim];

        for (int b = 0; b < batchSize; b++) {
            for (int h = 0; h < NUM_HEADS; h++) {
                double[][] scaledQK = matrixMultiply(q[b][h], transpose(k[b][h]));

                for (int i = 0; i < seqLen; i++) {
                    for (int j = 0; j < seqLen; j++) {
                        scaledQK[i][j] /= Math.sqrt(headDim);
                    }
                }

                if (masked) {
                    for (int i = 0; i < seqLen; i++) {
                        for (int j = i + 1; j < seqLen; j++) {
                            scaledQK[i][j] = Double.NEGATIVE_INFINITY;
                        }
                    }
                }

                double[][] attention = softmax(scaledQK);
                output[b][h] = matrixMultiply(attention, v[b][h]);
            }
        }
        return output;
    }

    //モデルの保存
    public void saveModel(File file) throws IOException {
        JSONObject modelJson = new JSONObject();

        // 重みとバイアスを保存
        JSONObject weightsJson = new JSONObject();
        for (Map.Entry<String, double[][]> entry : weights.entrySet()) {
            JSONArray matrixJson = new JSONArray();
            for (double[] row : entry.getValue()) {
                JSONArray rowJson = new JSONArray();
                for (double val : row) {
                    rowJson.put(val);
                }
                matrixJson.put(rowJson);
            }
            weightsJson.put(entry.getKey(), matrixJson);
        }
        modelJson.put("weights", weightsJson);

        JSONObject biasesJson = new JSONObject();
        for (Map.Entry<String, double[]> entry : biases.entrySet()) {
            JSONArray biasJson = new JSONArray();
            for (double val : entry.getValue()) {
                biasJson.put(val);
            }
            biasesJson.put(entry.getKey(), biasJson);
        }
        modelJson.put("biases", biasesJson);

        // ファイルに書き込み
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(modelJson.toString(2)); // インデント付きで書き込み
        }
    }

    //モデルの読み込み
    public void loadModel(File file) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
        JSONObject modelJson = new JSONObject(jsonContent);

        // 重みの読み込み
        JSONObject weightsJson = modelJson.getJSONObject("weights");
        Iterator<String> weightKeys = weightsJson.keys();
        while (weightKeys.hasNext()) {
            String key = weightKeys.next();
            JSONArray matrixJson = weightsJson.getJSONArray(key);
            int rows = matrixJson.length();
            int cols = matrixJson.getJSONArray(0).length();
            double[][] matrix = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                JSONArray rowJson = matrixJson.getJSONArray(i);
                for (int j = 0; j < cols; j++) {
                    matrix[i][j] = rowJson.getDouble(j);
                }
            }
            weights.put(key, matrix);
        }

        // バイアスの読み込み
        JSONObject biasesJson = modelJson.getJSONObject("biases");
        Iterator<String> biasKeys = biasesJson.keys();
        while (biasKeys.hasNext()) {
            String key = biasKeys.next();
            JSONArray biasJson = biasesJson.getJSONArray(key);
            int size = biasJson.length();
            double[] bias = new double[size];
            for (int i = 0; i < size; i++) {
                bias[i] = biasJson.getDouble(i);
            }
            biases.put(key, bias);
        }
    }

    // メインの推論メソッド
    public String generateText(Player player, String prompt, int maxLength) throws IOException {
        generatingPlayers.put(player.getUniqueId(), true);

        // 1. プロンプトのトークン化
        List<Integer> inputTokens = bpe.tokenize(prompt);
        int currentLength = inputTokens.size();

        // 2. 推論ループ
        while (currentLength < maxLength) {
            // 入力トークン列を2次元配列に変換
            double[][][] input = new double[1][currentLength][D_MODEL]; // バッチサイズ1
            for (int i = 0; i < currentLength; i++) {
                double[] embedding = word2Vec.getVector(String.valueOf(bpe.tokenize("").get(inputTokens.get(i)))); // トークンIDから単語を取得し、埋め込みベクトルに変換

                // 位置エンコーディングを加算
                double[] pe = positionalEncoding(i);
                for (int j = 0; j < D_MODEL; j++) {
                    input[0][i][j] = embedding[j] + pe[j];
                }
            }

            // 3. Transformerに通す
            double[][][] output = forward(input);

            // 4. 次のトークンの予測 (最後のトークンに対する予測のみを使用)
            double[] nextTokenProbs = output[0][currentLength - 1];

            // 5. Top-pサンプリング (ここでは単純なargmaxを使用)
            int nextTokenId = 0;
            double maxProb = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < nextTokenProbs.length; i++) {
                if (nextTokenProbs[i] > maxProb) {
                    maxProb = nextTokenProbs[i];
                    nextTokenId = i;
                }
            }

            // 6. 予測されたトークンIDを追加
            inputTokens.add(nextTokenId);
            currentLength++;

            // 終了条件 (ここでは<EOS>トークンを仮定)
            if (nextTokenId == 0) { // <EOS>トークンのIDを0と仮定
                break;
            }
        }

        // 7. トークンID列を文字列に変換
        List<String> outputWords = new ArrayList<>();
        Map<Integer, String> idToWord = new HashMap<>();
        try {
            JSONObject wordsJson = new JSONObject(new String(Files.readAllBytes(Paths.get(configDir, "words.json")), StandardCharsets.UTF_8));
            JSONObject vocab = wordsJson.getJSONObject("vocab");
            Iterator<String> keys = vocab.keys();
            while (keys.hasNext()) {
                String word = keys.next();
                int id = vocab.getInt(word);
                idToWord.put(id, word);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int tokenId : inputTokens) {
            outputWords.add(idToWord.get(tokenId));
        }
        return String.join("", outputWords); // 単語間にスペースを入れない
    }

    // Transformerの順伝播
    private double[][][] forward(double[][][] input) {
        // 入力: [batch_size, seq_len, d_model]

        // Decoder層
        double[][][] decoderOutput = input;
        for (int i = 0; i < NUM_LAYERS; i++) {
            // Masked Multi-Head Self-Attention
            double[][][] mhaOutput = layerNorm(residualConnection(decoderOutput, maskedMultiHeadAttention(decoderOutput, i)));

            // Feed Forward Network
            double[][][] ffnOutput = layerNorm(residualConnection(mhaOutput, feedForwardNetwork(mhaOutput, i)));

            decoderOutput = ffnOutput;
        }

        // 線形層 + Softmax
        double[][][] linearOutput = matrixMultiply(decoderOutput, weights.get("linear"));
        linearOutput = addBias(linearOutput, biases.get("linear"));

        // 正しい次元で output 配列を初期化
        double[][][] output = new double[linearOutput.length][linearOutput[0].length][bpe.tokenize("").size()];
        // 各バッチ、各トークン位置に対してsoftmaxを適用
        for (int i = 0; i < linearOutput.length; i++) {
            for (int j = 0; j < linearOutput[0].length; j++) {
                output[i][j] = softmax(linearOutput[i][j]); // 1次元配列に適用
            }
        }

        return output; // [batch_size, seq_len, vocab_size]
    }

    private double[] softmax(double[] input) {
        int length = input.length;
        double[] result = new double[length];
        double maxVal = Double.NEGATIVE_INFINITY;

        // 入力配列の最大値を求める
        for (int i = 0; i < length; i++) {
            if (input[i] > maxVal) {
                maxVal = input[i];
            }
        }

        double sumExp = 0.0;
        // 各要素を指数関数で変換し、合計を計算
        for (int i = 0; i < length; i++) {
            result[i] = Math.exp(input[i] - maxVal); // オーバーフロー対策
            sumExp += result[i];
        }

        // 各要素を合計で割り、確率分布にする
        for (int i = 0; i < length; i++) {
            result[i] /= sumExp;
        }

        return result;
    }

    private double[][][] layerNorm(double[][][] input) {
        int batchSize = input.length;
        int seqLen = input[0].length;
        int dModel = input[0][0].length;
        double[][][] output = new double[batchSize][seqLen][dModel];

        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {

                double mean = 0;
                for (int j = 0; j < dModel; j++) {
                    mean += input[b][i][j];
                }
                mean /= dModel;

                double variance = 0;
                for (int j = 0; j < dModel; j++) {
                    variance += Math.pow(input[b][i][j] - mean, 2);
                }
                variance /= dModel;

                double stddev = Math.sqrt(variance + 1e-5);

                for (int j = 0; j < dModel; j++) {
                    output[b][i][j] = (input[b][i][j] - mean) / stddev;
                }
            }
        }
        return output;
    }

    private final Map<UUID, Boolean> generatingPlayers = new HashMap<>();

    public boolean isGenerating(UUID playerId) {
        return generatingPlayers.getOrDefault(playerId, false);
    }

    public void removeGeneratingPlayer(UUID playerId) {
        generatingPlayers.remove(playerId);
    }


    // ソフトマックス関数
    private double[][] softmax(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] result = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            double maxVal = Arrays.stream(matrix[i]).max().orElse(Double.NEGATIVE_INFINITY);
            double[] exp = new double[cols];
            double sumExp = 0.0;

            // 指数計算と合計
            for (int j = 0; j < cols; j++) {
                exp[j] = Math.exp(matrix[i][j] - maxVal); // オーバーフロー対策
                sumExp += exp[j];
            }

            // ソフトマックス計算
            for (int j = 0; j < cols; j++) {
                result[i][j] = exp[j] / sumExp;
            }
        }

        return result;
    }

    //転置
    private double[][] transpose(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] transposed = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }

    // ReLU活性化関数
    private double[][][] relu(double[][][] matrix) {
        int batchSize = matrix.length;
        int seqLen = matrix[0].length;
        int dModel = matrix[0][0].length;

        double[][][] result = new double[batchSize][seqLen][dModel];
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < dModel; j++) {
                    result[b][i][j] = Math.max(0, matrix[b][i][j]);
                }
            }
        }
        return result;
    }

    // Residual Connection
    private double[][][] residualConnection(double[][][] input, double[][][] output) {
        int batchSize = input.length;
        int seqLen = input[0].length;
        int dModel = input[0][0].length; // inputとoutputのdModelは同じであると仮定
        double[][][] result = new double[batchSize][seqLen][dModel];

        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < dModel; j++) {
                    result[b][i][j] = input[b][i][j] + output[b][i][j];
                }
            }
        }
        return result;
    }

    // ヘッドに分割
    private double[][][][] splitHeads(double[][][] input, int numHeads) {
        int batchSize = input.length;
        int seqLen = input[0].length;
        int dModel = input[0][0].length;
        int headDim = dModel / numHeads;  // d_head を計算

        double[][][][] split = new double[batchSize][numHeads][seqLen][headDim];
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                for (int h = 0; h < numHeads; h++) {
                    System.arraycopy(input[b][i], h * headDim + 0, split[b][h][i], 0, headDim);
                }
            }
        }
        return split;
    }

    // ヘッドを結合
    private double[][][] concatenateHeads(double[][][][] input) {
        int batchSize = input.length;
        int numHeads = input[0].length;
        int seqLen = input[0][0].length;
        int headDim = input[0][0][0].length;
        int dModel = numHeads * headDim;

        double[][][] concatenated = new double[batchSize][seqLen][dModel];
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                for (int h = 0; h < numHeads; h++) {
                    System.arraycopy(input[b][h][i], 0, concatenated[b][i], h * headDim + 0, headDim);
                }
            }
        }
        return concatenated;
    }

    // 行列の乗算
    private double[][] matrixMultiply(double[][] a, double[][] b) {
        int aRows = a.length;
        int aCols = a[0].length;
        int bRows = b.length;
        int bCols = b[0].length;

        if (aCols != bRows) {
            throw new IllegalArgumentException("Matrix dimensions are not compatible for multiplication: " + aCols + " != " + bRows);
        }

        double[][] result = new double[aRows][bCols];
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bCols; j++) {
                for (int k = 0; k < aCols; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    //バイアスを加算
    private double[][][] addBias(double[][][] matrix, double[] bias) {
        int batchSize = matrix.length;
        int seqLen = matrix[0].length;
        int dModel = matrix[0][0].length; // dModel は bias の長さに等しい

        double[][][] result = new double[batchSize][seqLen][dModel];
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < seqLen; i++) {
                for (int j = 0; j < dModel; j++) {
                    result[b][i][j] = matrix[b][i][j] + bias[j];
                }
            }
        }
        return result;
    }
}
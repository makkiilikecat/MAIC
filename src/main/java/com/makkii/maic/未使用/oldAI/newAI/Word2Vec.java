package com.makkii.maic.oldAI.newAI;

import com.google.gson.stream.JsonReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class Word2Vec {

    public static final String AI_DATABASE_JSON_FILE = "ai_database.json";
    public static final String WORD2VEC_MODEL_FILE = "word2vec_model.json";
    public static final int EMBEDDING_DIM = 512;
    public static final int WINDOW_SIZE = 5; // コンテキストウィンドウサイズ
    public static final double LEARNING_RATE = 0.05;
    public static final int EPOCHS = 10;
    private final String configDir;
    private final Map<String, double[]> wordVectors;
    private final BPE bpe;

    public Word2Vec(String configDir) {
        this.configDir = configDir;
        this.wordVectors = new HashMap<>();
        this.bpe = new BPE(configDir);
    }

    // Word2Vecモデルの学習または読み込み
    public void loadOrTrainModel() throws IOException {
        File modelFile = new File(configDir, WORD2VEC_MODEL_FILE);
        if (modelFile.exists()) {
            loadModel(modelFile);
        } else {
            getLogger().info("[MAIC] トレーニングデータがないため、モデルをトレーニングします。");
            trainModel();
            saveModel(modelFile);
        }
    }

    // Word2Vecモデルの学習
    /*
    private void trainModel() throws IOException {
        // 1. ai_database.jsonの読み込み
        File jsonFile = new File(configDir, AI_DATABASE_JSON_FILE);
        if (!jsonFile.exists()) {
            throw new FileNotFoundException(AI_DATABASE_JSON_FILE + " not found in " + configDir);
        }
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath())), StandardCharsets.UTF_8);
        JSONArray jsonArray = new JSONArray(jsonContent);

        // 2. 全テキストのトークン化
        List<List<Integer>> tokenizedSentences = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject doc = jsonArray.getJSONObject(i);
            String text = doc.getString("text");
            List<Integer> tokens = bpe.tokenize(text); // BPEトークナイザーを使用
            tokenizedSentences.add(tokens);
        }

        // 3. 語彙の作成 (トークンID -> 単語) のマッピングも作成
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

        // 4. CBOWモデルの学習
        // 初期化 (ランダムなベクトル)
        Random random = new Random();
        for (int tokenId : idToWord.keySet()) {
            double[] vector = new double[EMBEDDING_DIM];
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                vector[i] = random.nextGaussian() * 0.01; // 小さな値で初期化
            }
            wordVectors.put(idToWord.get(tokenId), vector);
        }

        // 学習ループ
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            double totalLoss = 0;
            int wordCount = 0;

            for (List<Integer> sentence : tokenizedSentences) {
                for (int i = 0; i < sentence.size(); i++) {
                    int targetTokenId = sentence.get(i);
                    String targetWord = idToWord.get(targetTokenId);

                    // コンテキストの取得
                    List<String> contextWords = new ArrayList<>();
                    for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(sentence.size(), i + WINDOW_SIZE + 1); j++)
                    {
                        if (i != j) {
                            contextWords.add(idToWord.get(sentence.get(j)));
                        }
                    }

                    // CBOWの更新
                    double[] contextSum = new double[EMBEDDING_DIM];
                    for (String contextWord : contextWords) {
                        if (wordVectors.containsKey(contextWord)) {
                            double[] contextVector = wordVectors.get(contextWord);
                            for (int j = 0; j < EMBEDDING_DIM; j++) {
                                contextSum[j] += contextVector[j];
                            }
                        }
                    }

                    // 勾配降下法
                    if (wordVectors.containsKey(targetWord)) {
                        double[] targetVector = wordVectors.get(targetWord);
                        double[] error = new double[EMBEDDING_DIM];
                        //誤差計算
                        for (int j = 0; j < EMBEDDING_DIM; j++) {
                            error[j] = targetVector[j] - contextSum[j];
                        }
                        // パラメータの更新
                        for (int j = 0; j < EMBEDDING_DIM; j++) {
                            targetVector[j] -= LEARNING_RATE * error[j];
                        }
                        wordVectors.put(targetWord, targetVector);

                        //コンテキストのパラメータ更新
                        for (String contextWord : contextWords) {
                            if (wordVectors.containsKey(contextWord)) {
                                double[] contextVector = wordVectors.get(contextWord);
                                for (int j = 0; j < EMBEDDING_DIM; j++) {
                                    contextVector[j] -= LEARNING_RATE * error[j];
                                }
                                wordVectors.put(contextWord, contextVector);
                            }
                        }
                    }
                }
            }

            getLogger().info("Epoch " + (epoch + 1) + ", Loss: " + (totalLoss / wordCount));
        }
    }
    */
    private void trainModel() throws IOException {
        // 1. ai_database.jsonの読み込み (GsonのJsonReaderを使用)
        File jsonFile = new File(configDir, AI_DATABASE_JSON_FILE);
        if (!jsonFile.exists()) {
            throw new FileNotFoundException(AI_DATABASE_JSON_FILE + " not found in " + configDir);
        }

        List<List<Integer>> tokenizedSentences = new ArrayList<>();

        try (JsonReader reader = new JsonReader(new FileReader(jsonFile))) {
            reader.beginArray(); // JSON配列の開始

            while (reader.hasNext()) { // 各JSONオブジェクトを反復処理
                String text = null;
                reader.beginObject(); // JSONオブジェクトの開始
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("text")) {
                        text = reader.nextString();
                    } else {
                        reader.skipValue(); // "text" 以外のキーはスキップ
                    }
                }
                //getLogger().info("[MAIC] トークン化中...");
                reader.endObject(); // JSONオブジェクトの終了

                if (text != null) {
                    List<Integer> tokens = bpe.tokenize(text);
                    tokenizedSentences.add(tokens);
                }
            }

            reader.endArray(); // JSON配列の終了
        }

        getLogger().info("[MAIC] トークン化が完了しました。");

        // 3. 語彙の作成 (トークンID -> 単語) のマッピングも作成
        Map<Integer, String> idToWord = new HashMap<>();
        try (JsonReader wordsReader = new JsonReader(new FileReader(new File(configDir, "words.json")))) {
            wordsReader.beginObject();
            while (wordsReader.hasNext()) {
                String name = wordsReader.nextName();
                if (name.equals("vocab")) {
                    wordsReader.beginObject();
                    while (wordsReader.hasNext()) {
                        String word = wordsReader.nextName();
                        int id = wordsReader.nextInt();
                        idToWord.put(id, word);
                    }
                    wordsReader.endObject();
                } else {
                    wordsReader.skipValue();
                }
            }
            wordsReader.endObject();

        } catch (IOException e) {
            e.printStackTrace();
        }


        // 4. CBOWモデルの学習 (この部分は変更なし)
        Random random = new Random();
        for (int tokenId : idToWord.keySet()) {
            double[] vector = new double[EMBEDDING_DIM];
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                vector[i] = random.nextGaussian() * 0.01;
            }
            wordVectors.put(idToWord.get(tokenId), vector);
        }

        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            double totalLoss = 0;
            int wordCount = 0;

            for (List<Integer> sentence : tokenizedSentences) {
                for (int i = 0; i < sentence.size(); i++) {
                    int targetTokenId = sentence.get(i);
                    String targetWord = idToWord.get(targetTokenId);

                    List<String> contextWords = new ArrayList<>();
                    for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(sentence.size(), i + WINDOW_SIZE + 1); j++)
                    {
                        if (i != j) {
                            contextWords.add(idToWord.get(sentence.get(j)));
                        }
                    }

                    double[] contextSum = new double[EMBEDDING_DIM];
                    for (String contextWord : contextWords) {
                        if (wordVectors.containsKey(contextWord)) {
                            double[] contextVector = wordVectors.get(contextWord);
                            for (int j = 0; j < EMBEDDING_DIM; j++) {
                                contextSum[j] += contextVector[j];
                            }
                        }
                    }

                    if (wordVectors.containsKey(targetWord)) {
                        double[] targetVector = wordVectors.get(targetWord);
                        double[] error = new double[EMBEDDING_DIM];
                        for (int j = 0; j < EMBEDDING_DIM; j++) {
                            error[j] = targetVector[j] - contextSum[j];
                        }

                        for (int j = 0; j < EMBEDDING_DIM; j++) {
                            targetVector[j] -= LEARNING_RATE * error[j];
                        }
                        wordVectors.put(targetWord, targetVector);

                        for (String contextWord : contextWords) {
                            if (wordVectors.containsKey(contextWord)) {
                                double[] contextVector = wordVectors.get(contextWord);
                                for (int j = 0; j < EMBEDDING_DIM; j++) {
                                    contextVector[j] -= LEARNING_RATE * error[j];
                                }
                                wordVectors.put(contextWord, contextVector);
                            }
                        }
                    }
                }
            }
            getLogger().info("Epoch " + (epoch + 1) + ", Loss: " + (totalLoss / wordCount));
        }
    }

    // モデルの保存
    private void saveModel(File modelFile) throws IOException {
        JSONObject modelJson = new JSONObject();
        for (Map.Entry<String, double[]> entry : wordVectors.entrySet()) {
            JSONArray vectorJson = new JSONArray();
            for (double val : entry.getValue()) {
                vectorJson.put(val);
            }
            modelJson.put(entry.getKey(), vectorJson);
        }

        try (FileWriter fileWriter = new FileWriter(modelFile)) {
            fileWriter.write(modelJson.toString(2)); // インデント付きで書き込み
        }
    }

    // モデルの読み込み
    private void loadModel(File modelFile) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(modelFile.getAbsolutePath())), StandardCharsets.UTF_8);
        JSONObject modelJson = new JSONObject(jsonContent);

        Iterator<String> keys = modelJson.keys();
        while (keys.hasNext()) {
            String word = keys.next();
            JSONArray vectorJson = modelJson.getJSONArray(word);
            double[] vector = new double[EMBEDDING_DIM];
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                vector[i] = vectorJson.getDouble(i);
            }
            wordVectors.put(word, vector);
        }
    }

    // 単語ベクトルの取得
    public double[] getVector(String word) {
        return wordVectors.getOrDefault(word, new double[EMBEDDING_DIM]); // 存在しない場合はゼロベクトルを返す
    }
}
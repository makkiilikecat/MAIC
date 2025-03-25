//package com.makkii.maic.training;
//
//import java.io.*;
//import java.util.Random;
//
//import static com.makkii.maic.AIParams.*;
//
//public class AttentionTrainer {
//
//    /**
//     * Attention用の重み行列を学習(更新)します.
//     *
//     * @param inputEmbeddings   入力の埋め込み表現 (バッチサイズ x コンテキスト長 x 次元数)
//     * @param attentionGradients Attention機構から逆伝播された勾配
//     *                          [バッチサイズ][レイヤー][コンテキスト長][コンテキスト長][次元数]
//     * @param learningRate      学習率
//     */
//    public void train(double[][][] inputEmbeddings, double[][][][] attentionGradients, double learningRate) {
//        int batchSize = inputEmbeddings.length;
//        //int contextLength = inputEmbeddings[0].length; //コンテキストの長さ。train()の外で計算して渡した方が良い
//
//        for (int b = 0; b < batchSize; b++) { // バッチごとに処理
//            for (int l = 0; l < LAYERS; l++) { // レイヤーごとに処理
//                // 勾配から、重み行列の勾配を計算する部分。
//                // (本来、Attentionの計算中に求めておくべきだが、簡略化のため)
//                double[][] gradientsQ = calculateGradientForQ(attentionGradients[b][l]);
//                double[][] gradientsK = calculateGradientForK(attentionGradients[b][l]);
//                double[][] gradientsV = calculateGradientForV(attentionGradients[b][l]);
//
//
//                // 重み行列 Q, K, V の更新 (inputEmbeddings はここでは使わない)
//                for (int i = 0; i < DIMENSION; i++) {
//                    for (int j = 0; j < K_Q_DIMENTION; j++) {
//                        attentionWeightsQ[l][i][j] -= learningRate * gradientsQ[i][j];
//                        attentionWeightsK[l][i][j] -= learningRate * gradientsK[i][j];
//                        attentionWeightsV[l][i][j] -= learningRate * gradientsV[i][j];
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * attentionGradientsからattentionWeightsQの勾配を計算
//     * @param attentionGradients
//     * @return
//     */
//    private double[][] calculateGradientForQ(double[][][] attentionGradients) {
//        // attentionGradients[コンテキスト長][コンテキスト長][次元数]
//        // の最初のコンテキスト長のみ合計する
//        double[][] gradientsQ = new double[DIMENSION][K_Q_DIMENTION];
//        int contextLength = attentionGradients.length;
//
//        // ここでは単純な合計で勾配を近似 (本来はもっと複雑)
//        for (int i = 0; i < contextLength; i++) {
//            for (int j = 0; j < contextLength; j++) {
//                for (int k = 0; k < DIMENSION; k++) {
//                    // 実際は、attentionWeightsQを使って計算された値の勾配を使う
//                    gradientsQ[k][0] += attentionGradients[i][j][k];  //次元を合わせるため仮
//                }
//            }
//        }
//        return gradientsQ;
//    }
//
//    /**
//     * attentionGradientsからattentionWeightsKの勾配を計算
//     * @param attentionGradients
//     * @return
//     */
//    private double[][] calculateGradientForK(double[][][] attentionGradients) {
//        double[][] gradientsK = new double[dimension][kqDimension];
//        int contextLength = attentionGradients.length;
//        for (int i = 0; i < contextLength; i++) {
//            for (int j = 0; j < contextLength; j++) {
//                for (int k = 0; k < dimension; k++) {
//                    gradientsK[k][0] += attentionGradients[i][j][k]; //次元を合わせるため仮
//                }
//            }
//        }
//        return gradientsK;
//    }
//
//    /**
//     * attentionGradientsからattentionWeightsVの勾配を計算
//     * @param attentionGradients
//     * @return
//     */
//    private double[][] calculateGradientForV(double[][][] attentionGradients) {
//        double[][] gradientsV = new double[dimension][kqDimension];
//        int contextLength = attentionGradients.length;
//        for (int i = 0; i < contextLength; i++) {
//            for (int j = 0; j < contextLength; j++) {
//                for (int k = 0; k < dimension; k++) {
//                    gradientsV[k][0] += attentionGradients[i][j][k];  //次元を合わせるため仮
//                }
//            }
//        }
//        return gradientsV;
//    }
//
//    /**
//     * 指定されたレイヤーの重み行列Qを取得します.
//     *
//     * @param layer レイヤー番号
//     * @return 重み行列Q
//     */
//    public double[][] getWeightsQ(int layer) {
//        return attentionWeightsQ[layer];
//    }
//
//    /**
//     * 指定されたレイヤーの重み行列Kを取得します.
//     *
//     * @param layer レイヤー番号
//     * @return 重み行列K
//     */
//    public double[][] getWeightsK(int layer) {
//        return attentionWeightsK[layer];
//    }
//
//    /**
//     * 指定されたレイヤーの重み行列Vを取得します.
//     *
//     * @param layer レイヤー番号
//     * @return 重み行列V
//     */
//    public double[][] getWeightsV(int layer) {
//        return attentionWeightsV[layer];
//    }
//
//    /**
//     * Attentionの重み行列をファイルに保存します.
//     *
//     * @param filePath 保存先のファイルパス
//     * @throws IOException 入出力例外が発生した場合
//     */
//    public void save(String filePath) throws IOException {
//        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
//            oos.writeObject(new double[][][][]{attentionWeightsQ, attentionWeightsK, attentionWeightsV}); //まとめて保存
//        }
//    }
//
//    /**
//     * ファイルからAttentionの重み行列を読み込みます.
//     *
//     * @param filePath 読み込むファイルパス
//     * @throws IOException            入出力例外が発生した場合
//     * @throws ClassNotFoundException クラスが見つからない場合
//     */
//    public void load(String filePath) throws IOException, ClassNotFoundException {
//        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
//            double[][][][] weights = (double[][][][]) ois.readObject();
//            attentionWeightsQ = weights[0];
//            attentionWeightsK = weights[1];
//            attentionWeightsV = weights[2];
//        }
//    }
//
//    /**
//     * 既存の重み行列を設定します
//     * @param type
//     * @param layer
//     * @param weights
//     */
//    public void setAttentionWeights(String type, int layer, double[][] weights) {
//        switch (type) {
//            case "Q":
//                if (weights.length != dimension || weights[0].length != kqDimension) {
//                    throw new IllegalArgumentException("Q weights size is not match");
//                }
//                this.attentionWeightsQ[layer] = weights;
//                break;
//            case "K":
//                if (weights.length != dimension || weights[0].length != kqDimension) {
//                    throw new IllegalArgumentException("K weights size is not match");
//                }
//                this.attentionWeightsK[layer] = weights;
//                break;
//            case "V":
//                if (weights.length != dimension || weights[0].length != kqDimension) {
//                    throw new IllegalArgumentException("V weights size is not match");
//                }
//                this.attentionWeightsV[layer] = weights;
//                break;
//            default:
//                throw new IllegalArgumentException("Invalid weight type: " + type);
//        }
//    }
//    /**
//     * 現在のインスタンスの複製を返します
//     * @param type
//     * @param layer
//     * @return
//     */
//    public double[][] getAttentionWeights(String type, int layer) {
//        double[][] result = new double[dimension][kqDimension];
//        double[][][] target;
//        switch (type) {
//            case "Q":
//                target = attentionWeightsQ;
//                break;
//            case "K":
//                target = attentionWeightsK;
//                break;
//            case "V":
//                target = attentionWeightsV;
//                break;
//            default:
//                throw new IllegalArgumentException("Invalid weight type: " + type);
//        }
//        for(int i=0; i<dimension; ++i) {
//            System.arraycopy(target[layer][i], 0, result[i], 0, kqDimension);
//        }
//        return result;
//    }
//
//    // (任意) メモリ節約のため、不要になったら重み行列をnullにする
//    public void clear() {
//        attentionWeightsQ = null;
//        attentionWeightsK = null;
//        attentionWeightsV = null;
//    }
//
//    // テスト用 main メソッド (必要に応じて)
//    public static void main(String[] args) {
//        int layers = 6;
//        int dimension = 512;
//        int kqDimension = 64;
//        AttentionTrainer trainer = new AttentionTrainer(layers, dimension, kqDimension);
//        trainer.initialize();
//
//        // ダミーデータの作成 (本来はAttentionの計算結果と、そこからの逆伝播で得られた勾配)
//        double[][][] inputEmbeddings = new double[1][1][dimension]; // バッチサイズ1, コンテキスト長1
//        double[][][][] attentionGradients = new double[1][layers][1][1][dimension]; // バッチサイズ1
//
//        // 学習率
//        double learningRate = 0.001f;
//
//        // 学習の実行例
//        trainer.train(inputEmbeddings, attentionGradients, learningRate);
//
//        // 重み行列の一部を出力
//        double[][] qWeights = trainer.getWeightsQ(0); // 0番目のレイヤーのQ重み
//        for(int i=0; i<10; ++i){
//            System.out.println(qWeights[0][i] + " ");
//        }
//    }
//}
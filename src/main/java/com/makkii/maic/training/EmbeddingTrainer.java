package com.makkii.maic.training;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.makkii.maic.AIParams.*;

public class EmbeddingTrainer {

    /**
     * 埋め込み行列を学習(更新)します。
     *
     * @param inputIndices バッチ内の単語のインデックスの配列
     * @param gradients    対応する勾配の配列 (embeddingMatrix と同じ次元)
     * @param learningRate 学習率
     */
    public static void train(int[] inputIndices, double[][] gradients, double learningRate) {
        for (int i = 0; i < inputIndices.length; i++) {
            int wordIndex = inputIndices[i]; // 更新対象の単語のインデックス

            // inputIndicesのサイズとgradientsのサイズは同じため、
            // inputIndices.length == gradients.length になる
            double[] gradient = gradients[i];  // 対応する勾配

            // 勾配降下法による更新
            for (int j = 0; j < DIMENSION; j++) {
                embeddingMatrix[wordIndex][j] -= learningRate * gradient[j];
            }
        }
    }

    /**
     * 指定された単語インデックスの埋め込みベクトルを取得します.
     *
     * @param index 単語のインデックス
     * @return 埋め込みベクトル (double配列)
     */
    public static double[] getEmbedding(int index) {
        // 境界チェック(本番環境では例外処理などが推奨)
        if (index < 0 || index >= wordSize) {
            throw new IllegalArgumentException("Invalid word index: " + index);
        }
        return embeddingMatrix[index];
    }

    /**
     * 埋め込み行列をファイルに保存します.
     *
     * @param filePath 保存先のファイルパス
     * @throws IOException 入出力例外が発生した場合
     */
    public void save(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))) {
            oos.writeObject(embeddingMatrix);
        }
    }

    /**
     * ファイルから埋め込み行列を読み込みます.
     *
     * @param filePath 読み込むファイルパス
     * @throws IOException            入出力例外が発生した場合
     * @throws ClassNotFoundException クラスが見つからない場合
     */
    public void load(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Paths.get(filePath)))) {
            embeddingMatrix = (double[][]) ois.readObject();
        }
    }

    /**
     * 既存の埋め込み行列を設定します
     *
     * @param matrix
     */
    public void setEmbeddingMatrix(double[][] matrix) {
        if (matrix.length != wordSize || matrix[0].length != DIMENSION) {
            throw new IllegalArgumentException("matrix size is not match");
        }
        embeddingMatrix = matrix;
    }

    // テスト用 main メソッド (必要に応じて)
    public static void main(String[] args) {

        // ダミーデータの作成(本来は学習データから)
        int[] inputIndices = {0, 1, 2};
        double[][] gradients = {
                {0.1f, -0.2f, 0.05f, /*...*/}, // dimensionの数だけ要素が必要
                {-0.03f, 0.1f, -0.1f, /*...*/},
                {0.0f, 0.01f, -0.02f, /*...*/}
        };
        double learningRate = 0.01f;

        // 学習の実行例
        train(inputIndices, gradients, learningRate);

        //埋め込み行列の一部を出力
        double[] embedding = getEmbedding(0);
        for (int i = 0; i < 10; ++i) {
            System.out.print(embedding[i] + " ");
        }
    }
}
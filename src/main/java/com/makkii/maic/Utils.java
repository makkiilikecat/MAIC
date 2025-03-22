package com.makkii.maic;

import java.util.List;

public class Utils {


    //二次元配列をソフトマックス関数でノーマライズする
    public static float[][] softMax2D(float[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[][] result = new float[rows][cols];
        float maxVal = Float.NEGATIVE_INFINITY;

        // 各行の最大値を見つける
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] > maxVal) {
                    maxVal = matrix[i][j];
                }
            }
        }

        // ソフトマックス関数を計算する
        float sum = 0.0f;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = (float) Math.exp(matrix[i][j] - maxVal);
                sum += result[i][j];
            }
        }

        // 合計で割って正規化する
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] /= sum;
            }
        }

        return result;
    }

    // Softmax 関数 (ベクトル用)
    public static float[] softMax(float[] vector) {
        float[] result = new float[vector.length];
        float sum = 0.0f;
        float maxVal = Float.NEGATIVE_INFINITY;

        // 最大値を見つける
        for (float v : vector) {
            if (v > maxVal) {
                maxVal = v;
            }
        }

        // Softmax 計算
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) Math.exp(vector[i] - maxVal);
            sum += result[i];
        }

        // 正規化
        for (int i = 0; i < vector.length; i++) {
            result[i] /= sum;
        }

        return result;
    }
    public static float calculateLoss(float[][] output,  List<Integer> batchWordIds) {
        //本来であれば、交差エントロピー誤差を用いるが、
        //出力の次元と、教師データの形式が異なるため、今回は省略
        return 0;
    }

    public static float[][] calculateGradients(float loss, float[][] output, List<Integer> batchWordIds) {
        //本来であれば、誤差逆伝播法を用いて、各パラメータの勾配を計算するが、
        //今回は省略
        return null;
    }
    public static void updateParameters(float[][] gradients) {
        //本来的には、勾配を用いて、重み行列、バイアス、埋め込み行列を更新する
        //例：
        //embeddingMatrix -= LEARNING_RATE * gradients;
        //今回は省略
    }
}

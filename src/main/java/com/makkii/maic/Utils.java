package com.makkii.maic;

import java.util.Arrays;

public enum Utils {
    ;


    //二次元配列をソフトマックス関数でノーマライズする
    public static void softMax2D(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        //double[][] result = new double[rows][cols];
        double maxVal = Double.NEGATIVE_INFINITY;

        // 各行の最大値を見つける
        for (double[] doubles : matrix) {
            for (int j = 0; j < cols; j++) {
                if (doubles[j] > maxVal) {
                    maxVal = doubles[j];
                }
            }
        }

        // ソフトマックス関数を計算する
        double sum = 0.0f;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = Math.exp(matrix[i][j] - maxVal);
                sum += matrix[i][j];
            }
        }

        // 合計で割って正規化する
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] /= sum;
            }
        }

        return;
    }

    // Softmax 関数 (ベクトル用)
    public static void softMax(double[] vector) {
        double sum = 0.0f;
        double maxVal = Double.NEGATIVE_INFINITY;

        // 最大値を見つける
        for (double v : vector) {
            if (v > maxVal) {
                maxVal = v;
            }
        }

        // Softmax 計算
        for (int i = 0; i < vector.length; i++) {
            vector[i] = Math.exp(vector[i] - maxVal);
            sum += vector[i];
        }

        // 正規化
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= sum;
        }

        return;
    }

    /**
     * softmax関数の勾配計算
     *
     * @param d_softmax_input 　softmax関数への入力に関する勾配
     * @param softmax_output  softmax関数の出力
     * @return softmax関数の入力に関する勾配
     */
    public static double[] softmaxGradient(double[] d_softmax_input, double[] softmax_output) {
        int n = softmax_output.length;
        double[] d_input = new double[n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    d_input[i] += softmax_output[i] * (1 - softmax_output[i]) * d_softmax_input[j];
                } else {
                    d_input[i] += -softmax_output[i] * softmax_output[j] * d_softmax_input[j];
                }
            }
        }
        return d_input;
    }

    //二次元配列版
    public static double[][] softMax(double[][] input) {
        int rows = input.length;
        int cols = input[0].length;
        double[][] result = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            double max = Arrays.stream(input[i]).max().orElse(1.0);
            double[] exp = Arrays.stream(input[i]).map(x -> Math.exp(x - max)).toArray();
            double sum = Arrays.stream(exp).sum();
            result[i] = Arrays.stream(exp).map(x -> x / sum).toArray();
        }

        return result;
    }
}

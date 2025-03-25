package com.makkii.maic.training;

public class LossCalculator {

    /**
     * 交差エントロピー損失を計算する
     *
     * @param predictedProbabilities TextGeneratorからの出力（各語彙の確率分布）
     * @param targetIndex            正解トークンのインデックス
     * @return 交差エントロピー損失
     */
    public static double calculateCrossEntropyLoss(double[] predictedProbabilities, int targetIndex) {
        // 予測確率が0になるのを防ぐために、微小な値を加える（数値的安定性のため）
        double predictedProbability = predictedProbabilities[targetIndex] + 1e-15;

        // 交差エントロピー損失の計算
        double loss = -StrictMath.log(predictedProbability);

        return loss;
    }
}
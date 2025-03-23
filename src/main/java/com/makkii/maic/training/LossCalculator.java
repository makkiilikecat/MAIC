package com.makkii.maic.training;

import com.makkii.maic.AIParams;
import com.makkii.maic.Utils;

public class LossCalculator {

    /**
     * 交差エントロピー損失を計算します。
     *
     * @param predictedProbabilities モデルの出力 (各単語の予測確率分布)。
     *                             形状は [バッチサイズ, コンテキスト長, 語彙サイズ]。
     * @param targetTokenIds       正解の単語 ID。形状は [バッチサイズ, コンテキスト長]。
     * @return 交差エントロピー損失 (スカラー値)。
     */
    public static float calculateLoss(float[][][] predictedProbabilities, int[][] targetTokenIds) {
        int batchSize = predictedProbabilities.length;
        int contextSize = predictedProbabilities[0].length;
        float totalLoss = 0.0f;

        for (int batch = 0; batch < batchSize; batch++) {
            for (int context = 0; context < contextSize; context++) {
                int targetTokenId = targetTokenIds[batch][context];

                // 正解トークンに対応する予測確率を取得
                float predictedProbability = predictedProbabilities[batch][context][targetTokenId];

                // 損失を計算 (正解ラベルの確率は1なので、log(predictedProbability)だけで良い)
                // -log(0) を避けるために、微小な値を加算
                totalLoss += -Math.log(predictedProbability + 1e-15f);
            }
        }

        // バッチ全体の平均損失を返す
        return totalLoss / (batchSize * contextSize);
    }

    /**
     *  ソフトマックス関数を用いて確率分布に変換し、交差エントロピー損失を計算
     * @param predictedLogits
     * @param targetTokenIds
     * @return
     */
    public static float calculateLossWithSoftmax(float[][][] predictedLogits, int[][] targetTokenIds) {
        int batchSize = predictedLogits.length;
        int contextSize = predictedLogits[0].length;
        float[][][] predictedProbabilities = new float[batchSize][contextSize][AIParams.wordSize];
        float totalLoss = 0.0f;

        for(int batch = 0; batch < batchSize; batch++){
            for(int context = 0; context < contextSize; context++){
                // ソフトマックス関数を適用して確率分布に変換
                predictedProbabilities[batch][context] = Utils.softMax(predictedLogits[batch][context]);

                // 正解トークンに対応する予測確率を取得し、損失を計算
                int targetTokenId = targetTokenIds[batch][context];
                // -log(0) を避けるために、微小な値を加算
                totalLoss += -Math.log(predictedProbabilities[batch][context][targetTokenId] + 1e-15f);
            }
        }
        // バッチ全体の平均損失を返す
        return totalLoss / (batchSize * contextSize);
    }
}
package com.makkii.maic.training;

import java.util.Arrays;
import java.util.Random;

import static com.makkii.maic.AIParams.*;

public enum Initializer {
    ;

    public static void initializeModel() {

        // 埋め込み行列の初期化 (ランダムな値で)
        embeddingMatrix = new float[wordSize][DIMENSION];
        unEmbeddingMatrix = new float[DIMENSION][wordSize];
        // He の初期化の場合はこっち
        // float stddev = (float) Math.sqrt(2.0 / wordSize);
        // Xavier/Glorot の初期化(標準偏差を計算)
        float stddev = (float) Math.sqrt(2.0 / (wordSize + DIMENSION));

        Random random = new Random();

        for (int i = 0; i < wordSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                // 小さな値で初期化
                //embeddingMatrix[i][j] = (float) (random.nextGaussian() * 0.01);
                // 正規分布に従う乱数で初期化
                embeddingMatrix[i][j] = (float) (random.nextGaussian() * stddev);
                unEmbeddingMatrix[j][i] = (float) (random.nextGaussian() * stddev);
            }
        }

        // attention, multiplayerPerceptron 用のパラメータを初期化
        attentionWeightsQ = new float[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsK = new float[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsV = new float[LAYERS][MAX_CONTEXT][DIMENSION];
        mppWeights1 = new float[LAYERS][DIMENSION][DIMENSION * 4];
        mppBiases1 = new float[LAYERS][DIMENSION * 4];
        mppWeights2 = new float[LAYERS][DIMENSION * 4][DIMENSION];
        mppBiases2 = new float[LAYERS][DIMENSION];

        // 全てのパラメータをランダム初期化
        initializeWeights(attentionWeightsQ, random);
        initializeWeights(attentionWeightsK, random);
        initializeWeights(attentionWeightsV, random);

        initializeWeights(mppWeights1, random);
        initializeBiases(mppBiases1, random);
        initializeWeights(mppWeights2, random);
        initializeBiases(mppBiases2, random);
    }

    public static void initializeWeights(float[][][] weights, Random random) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                for (int k = 0; k < weights[i][j].length; k++) {
                    weights[i][j][k] = (float) (random.nextGaussian() * 0.01);
                }
            }
        }
    }

    public static void initializeBiases(float[][] biases, Random random) {
        // バイアスは0で初期化
        for (float[] biase : biases) Arrays.fill(biase, 0.0f);
    }
}

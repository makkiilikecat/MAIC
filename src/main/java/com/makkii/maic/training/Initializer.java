package com.makkii.maic.training;

import java.util.Arrays;
import java.util.Random;

import static com.makkii.maic.AIParams.*;

public enum Initializer {
    ;

    public static void initializeModel() {

        Random random = new Random();

        // 埋め込み行列の初期化 (ランダムな値で)
        embeddingMatrix = new double[wordSize][DIMENSION];
        unEmbeddingMatrix = new double[DIMENSION][wordSize];
        embeddingMatrixGradients = new double[wordSize][DIMENSION];
        unEmbeddingMatrixGradients = new double[DIMENSION][wordSize];
        // He の初期化の場合はこっち
        // double stddev = (double) Math.sqrt(2.0 / wordSize);
        // Xavier/Glorot の初期化(標準偏差を計算)
        double stddev = Math.sqrt(2.0 / (wordSize + DIMENSION));
        for (int i = 0; i < wordSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                // 小さな値で初期化
                //embeddingMatrix[i][j] = (double) (random.nextGaussian() * 0.01);
                // 正規分布に従う乱数で初期化
                embeddingMatrix[i][j] = random.nextGaussian() * stddev;
                unEmbeddingMatrix[j][i] = random.nextGaussian() * stddev;
                embeddingMatrixGradients[i][j] = random.nextGaussian() * stddev;
                unEmbeddingMatrixGradients[j][i] = random.nextGaussian() * stddev;
            }
        }

        // attention, multiplayerPerceptron 用のパラメータを初期化
        attentionWeightsQ = new double[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsK = new double[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsV = new double[LAYERS][MAX_CONTEXT][DIMENSION];

        initializeWeights(attentionWeightsQ, random);
        initializeWeights(attentionWeightsK, random);
        initializeWeights(attentionWeightsV, random);


        mppWeights1 = new double[LAYERS][DIMENSION][DIMENSION * 4];
        mppBiases1 = new double[LAYERS][DIMENSION * 4];
        mppWeights2 = new double[LAYERS][DIMENSION * 4][DIMENSION];
        mppBiases2 = new double[LAYERS][DIMENSION];

        initializeWeights(mppWeights1, random);
        initializeBiases(mppBiases1, random);
        initializeWeights(mppWeights2, random);
        initializeBiases(mppBiases2, random);

        attentionWeightsQGradients = new double[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsKGradients = new double[LAYERS][MAX_CONTEXT][DIMENSION];
        attentionWeightsVGradients = new double[LAYERS][MAX_CONTEXT][DIMENSION];

        initializeWeights(attentionWeightsQGradients, random);
        initializeWeights(attentionWeightsKGradients, random);
        initializeWeights(attentionWeightsVGradients, random);


        mppWeights1Gradients = new double[LAYERS][DIMENSION][DIMENSION * 4];
        mppBiases1Gradients = new double[LAYERS][DIMENSION * 4];
        mppWeights2Gradients = new double[LAYERS][DIMENSION * 4][DIMENSION];
        mppBiases2Gradients = new double[LAYERS][DIMENSION];

        initializeWeights(mppWeights1Gradients, random);
        initializeBiases(mppBiases1Gradients, random);
        initializeWeights(mppWeights2Gradients, random);
        initializeBiases(mppBiases2Gradients, random);
    }

    public static void initializeWeights(double[][][] weights, Random random) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                for (int k = 0; k < weights[i][j].length; k++) {
                    weights[i][j][k] = random.nextGaussian() * 0.1;
                    //getLogger().info("initializeWeights: " + weights[i][j][k]);
                }
            }
        }
    }

    public static void initializeBiases(double[][] biases, Random random) {
        // バイアスは0で初期化
        for (double[] biase : biases) Arrays.fill(biase, 0.0f);
    }
}

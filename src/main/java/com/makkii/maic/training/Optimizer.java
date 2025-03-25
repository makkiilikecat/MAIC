package com.makkii.maic.training;

import java.util.Arrays;

import static com.makkii.maic.AIParams.*;

public class Optimizer {

    /**
     * 確率的勾配降下法 (SGD) を用いてパラメータを更新します。
     *
     * @param learningRate 学習率
     */
    public static void updateParameters(double learningRate) {

        // embeddingMatrix の更新
        for (int i = 0; i < wordSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                embeddingMatrix[i][j] -= learningRate * embeddingMatrixGradients[i][j];
                embeddingMatrixGradients[i][j] = 0; // 勾配リセット
            }
        }

        // unEmbeddingMatrix の更新
        for (int i = 0; i < DIMENSION; i++) {
            for (int j = 0; j < wordSize; j++) {
                unEmbeddingMatrix[i][j] -= learningRate * unEmbeddingMatrixGradients[i][j];
                unEmbeddingMatrixGradients[i][j] = 0; // 勾配リセット
            }
        }

        // attentionWeightsQ の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < MAX_CONTEXT; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsQ[i][j][k] -= learningRate * attentionWeightsQGradients[i][j][k];
                    attentionWeightsQGradients[i][j][k] = 0; // 勾配リセット
                }
            }
        }

        // attentionWeightsK の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < MAX_CONTEXT; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsK[i][j][k] -= learningRate * attentionWeightsKGradients[i][j][k];
                    attentionWeightsKGradients[i][j][k] = 0; // 勾配リセット
                }
            }
        }

        // attentionWeightsV の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < MAX_CONTEXT; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsV[i][j][k] -= learningRate * attentionWeightsVGradients[i][j][k];
                    attentionWeightsVGradients[i][j][k] = 0; // 勾配リセット
                }
            }
        }

        // mppWeights1 の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION * 4; k++) {
                    mppWeights1[i][j][k] -= learningRate * mppWeights1Gradients[i][j][k];
                    mppWeights1Gradients[i][j][k] = 0; // 勾配リセット
                }
            }
        }

        // mppBiases1 の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                mppBiases1[i][j] -= learningRate * mppBiases1Gradients[i][j];
                mppBiases1Gradients[i][j] = 0; // 勾配リセット
            }
        }

        // mppWeights2 の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    mppWeights2[i][j][k] -= learningRate * mppWeights2Gradients[i][j][k];
                    mppWeights2Gradients[i][j][k] = 0; // 勾配リセット
                }
            }
        }

        // mppBiases2 の更新
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                mppBiases2[i][j] -= learningRate * mppBiases2Gradients[i][j];
                mppBiases2Gradients[i][j] = 0; // 勾配リセット
            }
        }
    }



    // 勾配をリセットするメソッド
    public static void resetGradients() {
        // embeddingMatrixGradients のリセット
        for (int i = 0; i < wordSize; i++) {
            Arrays.fill(embeddingMatrixGradients[i], 0);
        }
        for (int i = 0; i < DIMENSION; i++){
            Arrays.fill(unEmbeddingMatrixGradients[i], 0);
        }

        // 他のパラメータの勾配も同様にリセット
        for (int i = 0; i < LAYERS; i++) {
            for (int j = 0; j < MAX_CONTEXT; j++) {
                Arrays.fill(attentionWeightsQGradients[i][j], 0);
                Arrays.fill(attentionWeightsKGradients[i][j], 0);
                Arrays.fill(attentionWeightsVGradients[i][j], 0);
            }
        }

        for(int i = 0; i < LAYERS; i++){
            for(int j = 0; j < DIMENSION; j++){
                Arrays.fill(mppWeights1Gradients[i][j], 0);
            }
        }

        for (int i = 0; i < LAYERS; i++) {
            Arrays.fill(mppBiases1Gradients[i], 0);
        }

        for(int i = 0; i < LAYERS; i++){
            for(int j = 0; j < DIMENSION * 4; j++){
                Arrays.fill(mppWeights2Gradients[i][j], 0);
            }
        }
        for (int i = 0; i < LAYERS; i++){
            Arrays.fill(mppBiases2Gradients[i], 0);
        }
    }
}
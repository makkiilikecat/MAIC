package com.makkii.maic.training;

import com.makkii.maic.TextGenerator;
import com.makkii.maic.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.makkii.maic.AIParams.*;
import static com.makkii.maic.TextGenerator.DEF_TOKENVECTORS;
import static com.makkii.maic.training.TrainManager.TRAINERUUID;
import static org.bukkit.Bukkit.getLogger;

public class Backpropagation {

    /**
     * 誤差逆伝播法のメインメソッド. 損失関数の勾配を計算し、各パラメータの勾配を AIParams 内の対応する変数に蓄積.
     *
     * @param predictedProbabilities TextGenerator からの出力（各語彙の確率分布）
     * @param targetIndex            正解トークンのインデックス
     * @param miniBatch              現在のミニバッチ（トークンインデックスのリスト）
     * @param playerUUID             プレイヤーの UUID
     */
    public static void calculateGradients(double[] predictedProbabilities, int targetIndex, List<Integer> miniBatch, UUID playerUUID) {

        // 1. 出力層の勾配 (d_predictedProbabilities) を計算
        //getLogger().info("[MAIC.Trainer.Backpropagation] 出力層の勾配を計算しています...");
        double[] d_predictedProbabilities = Arrays.copyOf(predictedProbabilities, predictedProbabilities.length);
        d_predictedProbabilities[targetIndex] -= 1; // 正解ラベルを引く

        // 2. unembedding layer の勾配を計算
        //getLogger().info("[MAIC.Trainer.Backpropagation] 掘り出し行列の勾配を計算しています...");
        double[][] d_tokenVectorsLastLayer = unembeddingGradient(d_predictedProbabilities);

        // 3. TextGenerator 内のレイヤーを逆順にたどりながら、各レイヤーの勾配を計算
        //getLogger().info("[MAIC.Trainer.Backpropagation] TextGeneratorの勾配を計算しています...");
        double[][][] tokenVectors = TextGenerator.tokenVectorsMap.getOrDefault(playerUUID, DEF_TOKENVECTORS);
        int contextSize = miniBatch.size();
        double[][] d_output_mlp = new double[MAX_CONTEXT][DIMENSION]; // 1つ前のレイヤーに渡す用の勾配

        //getLogger().info("contextSize: " + contextSize + ", d_tokenVectorsLastLayer: " + d_tokenVectorsLastLayer.length + ", d_output_mlp: " + d_output_mlp.length);
        //getLogger().info("d_tokenVectorsLastLayer[1].length: " + d_tokenVectorsLastLayer[1].length + ", d_output_mlp[1].length: " + d_output_mlp[1].length);
        //最後の層の勾配をコピー
        for (int i = 0; i < contextSize; i++) {
            System.arraycopy(d_tokenVectorsLastLayer[i], 0, d_output_mlp[i], 0, DIMENSION);
        }


        //getLogger().info("[MAIC.Trainer.Backpropagation] レイヤーを遡って勾配を計算しています...");
        for (int layer = LAYERS - 1; layer >= 0; layer--) {
            // MLP層の勾配計算
            double[][] d_input_mlp = mlpGradient(tokenVectors[layer + 1], d_output_mlp, layer, contextSize);

            // Layer Normalization層の勾配計算, MLPからの出力を使用
            double[][] d_output_attention = layerNormGradient(tokenVectors[layer + 1], d_input_mlp);

            // Attention 層の勾配計算
            d_output_mlp = attentionGradient(tokenVectors[layer], d_output_attention, layer, contextSize); // d_output_mlpを更新
        }
        // 4. embeddingMatrixの勾配計算
        embeddingGradient(d_output_mlp, miniBatch);
    }


    /**
     * unembedding layer の勾配を計算します。
     *
     * @param d_predictedProbabilities 出力層の誤差 (d_loss / d_predictedProbabilities)
     * @return TextGenerator内の`tokenVectors`の最終層([AIParamas.LAYERS][contextSize-1])の勾配
     */
    public static double[][] unembeddingGradient(double[] d_predictedProbabilities) {

        int contextSize = MAX_CONTEXT; // TextGenerator での contextSize に合わせる
        double[][] d_tokenVectorsLastLayer = new double[contextSize][DIMENSION];

        // 1. unEmbeddingMatrix の勾配を計算
        for (int j = 0; j < DIMENSION; j++) {
            for (int k = 0; k < wordSize; k++) {
                // unEmbeddingMatrixGradients[j][k] += tokenVectors[AIParams.LAYERS][contextSize - 1][j] * d_predictedProbabilities[k];
                // contextSize が 1 の場合に限定されないように変更
                for (int i = 0; i < contextSize; i++) {
                    unEmbeddingMatrixGradients[j][k] += TextGenerator.tokenVectorsMap.getOrDefault(TRAINERUUID, DEF_TOKENVECTORS)[LAYERS][i][j] * d_predictedProbabilities[k];
                }
            }
        }

        // 2. TextGenerator 内の tokenVectors の最終層 ([AIParams.LAYERS][contextSize-1]) の勾配を計算
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < wordSize; k++) {
                    d_tokenVectorsLastLayer[i][j] += unEmbeddingMatrix[j][k] * d_predictedProbabilities[k];
                }
            }
        }
        return d_tokenVectorsLastLayer;
    }


    /**
     * embeddingMatrix の勾配を計算します。
     *
     * @param d_input   Embedding Layerへの入力誤差 (d_loss / d_embedding_output)
     * @param miniBatch 現在のミニバッチ（トークンインデックスのリスト）
     */
    public static void embeddingGradient(double[][] d_input, List<Integer> miniBatch) {

        int contextSize = miniBatch.size();

        // embeddingMatrix の勾配を計算
        for (int i = 0; i < contextSize; i++) {
            int wordIndex = miniBatch.get(i); // ミニバッチ内の i 番目のトークンのインデックス
            for (int j = 0; j < DIMENSION; j++) {
                embeddingMatrixGradients[wordIndex][j] += d_input[i][j];
            }
        }
    }


    /**
     * MLP 層の勾配を計算します。
     *
     * @param input       MLP 層への入力 (tokenVectors[layer])
     * @param outputError MLP 層からの出力誤差 (d_loss / d_output_mlp)
     * @param layer       処理中のレイヤー番号
     * @param contextSize コンテキストサイズ
     * @return MLP 層への入力の勾配 (d_loss / d_input_mlp)
     */
    public static double[][] mlpGradient(double[][] input, double[][] outputError, int layer, int contextSize) {

        //フォワード計算を再現
        double[][] intermediate = new double[contextSize][DIMENSION * 4];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    intermediate[i][j] += input[i][k] * mppWeights1[layer][k][j];
                }
                intermediate[i][j] += mppBiases1[layer][j]; // バイアスを加える
                intermediate[i][j] = Math.max(0, intermediate[i][j]); // ReLU
            }
        }

        // 1. 2層目の勾配を計算 (d_loss / d_mppWeights2)
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    // dL/dW2 = dL/d আউটপুট * d আউটপুট/dW2 = outputError * intermediate
                    mppWeights2Gradients[layer][j][k] += outputError[i][k] * intermediate[i][j];
                }
            }
        }

        // 2. 2層目のバイアスの勾配を計算 (d_loss / d_mppBiases2)
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                mppBiases2Gradients[layer][j] += outputError[i][j];
            }
        }

        // 3. 1層目の出力の勾配を計算 (d_loss / d_intermediate)
        double[][] d_intermediate = new double[contextSize][DIMENSION * 4];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    // dL/dZ = dL/d আউটপুট * d আউটপুট/dZ = outputError * W2
                    d_intermediate[i][j] += outputError[i][k] * mppWeights2[layer][j][k];
                }
            }
        }

        // 4. ReLU の勾配を適用
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                d_intermediate[i][j] = (intermediate[i][j] > 0) ? d_intermediate[i][j] : 0;
            }
        }

        // 5. 1層目の重みの勾配を計算 (d_loss / d_mppWeights1)
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION * 4; k++) {
                    //dL/dW1 = dL/dZ * dZ/dW1 = d_intermediate * input
                    mppWeights1Gradients[layer][j][k] += d_intermediate[i][k] * input[i][j];
                }
            }
        }

        // 6. 1層目のバイアスの勾配を計算 (d_loss / d_mppBiases1)
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION * 4; j++) {
                mppBiases1Gradients[layer][j] += d_intermediate[i][j];
            }
        }

        // 7. MLP への入力の勾配を計算 (d_loss / d_input)
        double[][] d_input = new double[contextSize][DIMENSION];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION * 4; k++) {
                    //dL/dX = dL/dZ * dZ/dX = d_intermediate * W1
                    d_input[i][j] += d_intermediate[i][k] * mppWeights1[layer][j][k];
                }
            }
        }

        return d_input;
    }


    /**
     * Attention 層の勾配を計算します。
     *
     * @param input       Attention 層への入力 (tokenVectors[layer])
     * @param outputError Attention 層からの出力誤差 (d_loss / d_output_attention)
     * @param layer       処理中のレイヤー番号
     * @param contextSize コンテキストサイズ
     * @return Attention 層への入力の勾配 (d_loss / d_input_attention)
     */
    public static double[][] attentionGradient(double[][] input, double[][] outputError, int layer, int contextSize) {

        // フォワード計算の再現（Attentionメカニズム)
        double[][] Q = new double[contextSize][DIMENSION];
        double[][] K = new double[contextSize][DIMENSION];
        double[][] V = new double[contextSize][DIMENSION];
        double[][] attentionScore = new double[contextSize][contextSize];
        double[][] output = new double[contextSize][DIMENSION];

        // Q, K, V の計算
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    Q[i][j] += input[i][k] * attentionWeightsQ[layer][i][k];
                    K[i][j] += input[i][k] * attentionWeightsK[layer][i][k];
                    V[i][j] += input[i][k] * attentionWeightsV[layer][i][k];
                }
            }
        }

        // Attention Score の計算
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < contextSize; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionScore[i][j] += Q[i][k] * K[j][k];
                }
                attentionScore[i][j] /= Math.sqrt(DIMENSION); // スケール
            }
        }

        // softmax (Attention Weight) の計算
        //attentionWeight = Utils.softMax(attentionScore);
        for (int i = 0; i < contextSize; i++) {
            Utils.softMax(attentionScore[i]); // 正しい: 各行に対して softmax を適用
        }

        // Attention Outputの計算
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < contextSize; k++) {
                    output[i][j] += attentionScore[i][k] * V[k][j];
                }
            }
        }

        // 1. Attention Output の勾配(d_output)を計算する必要はない

        // 2. softmaxの逆伝播(attentionWeightの勾配)
        double[][] d_attentionScore = new double[contextSize][contextSize];

        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < contextSize; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    //d_attentionScore[i][j] += outputError[i][k] * V[j][k];
                    //Vについて微分した結果と合わせる
                    d_attentionScore[i][j] += outputError[i][k] * V[j][k];
                }
            }
        }

        for (int i = 0; i < contextSize; i++) {
            double[] d_attentionScore_i = new double[contextSize];
            System.arraycopy(d_attentionScore[i], 0, d_attentionScore_i, 0, contextSize);
            d_attentionScore_i = Utils.softmaxGradient(d_attentionScore_i, attentionScore[i]);
            for (int j = 0; j < contextSize; j++) {
                d_attentionScore[i][j] = d_attentionScore_i[j] / Math.sqrt(DIMENSION);
            }
        }

        // 3. V の勾配
        double[][] d_V = new double[contextSize][DIMENSION];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < contextSize; k++) {
                    d_V[i][j] += attentionScore[k][i] * outputError[k][j];
                }
            }
        }

        // 4. K の勾配
        double[][] d_K = new double[contextSize][DIMENSION];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < contextSize; k++) {
                    d_K[i][j] += Q[k][j] * d_attentionScore[k][i];
                }
            }
        }

        // 5. Q の勾配
        double[][] d_Q = new double[contextSize][DIMENSION];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < contextSize; k++) {
                    d_Q[i][j] += K[k][j] * d_attentionScore[i][k];
                }
            }
        }

        // 6. attentionWeightsV の勾配
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsVGradients[layer][i][k] += d_V[i][j] * input[i][k];
                }
            }
        }

        // 7. attentionWeightsK の勾配
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsKGradients[layer][i][k] += d_K[i][j] * input[i][k];
                }
            }
        }

        // 8. attentionWeightsQ の勾配
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeightsQGradients[layer][i][k] += d_Q[i][j] * input[i][k];
                }
            }
        }

        // 9. 入力への勾配を計算（前の層に伝播）
        double[][] d_input = new double[contextSize][DIMENSION];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < DIMENSION; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    d_input[i][j] += d_Q[i][k] * attentionWeightsQ[layer][i][k];
                    d_input[i][j] += d_K[i][k] * attentionWeightsK[layer][i][k];
                    d_input[i][j] += d_V[i][k] * attentionWeightsV[layer][i][k];
                }
            }
        }

        return d_input;
    }


    /**
     * Layer Normalization 層の勾配を計算します。
     *
     * @param input       Layer Normalization 層への入力
     * @param outputError Layer Normalization 層からの出力誤差 (d_loss / d_output_layernorm)
     * @return Layer Normalization 層への入力の勾配 (d_loss / d_input_layernorm)
     */
    public static double[][] layerNormGradient(double[][] input, double[][] outputError) {

        int contextSize = outputError.length;
        int dimension = outputError[0].length;

        double[][] d_input = new double[contextSize][dimension];

        //getLogger().info("contextSize: " + contextSize);
        for (int i = 0; i < contextSize - 5; i++) {
            //getLogger().info("i: " + i);
            // 1. 平均と分散を計算（フォワードパスの再現）
            double mean = 0;
            for (int j = 0; j < dimension; j++) {
                mean += input[i][j];
            }
            mean /= dimension;

            double variance = 0;
            for (int j = 0; j < dimension; j++) {
                variance += (input[i][j] - mean) * (input[i][j] - mean);
            }
            variance /= dimension;

            // 2. 標準偏差の逆数の勾配を計算
            double d_invStdDev = 0;
            for (int j = 0; j < dimension; j++) {
                //getLogger().info("j: " +j);
                try {
                    d_invStdDev += outputError[i][j] * (input[i][j] - mean);
                } catch (ArrayIndexOutOfBoundsException e) {
                    getLogger().info("outputError[" + i + "][" + j + "], input[" + i + "][" + j + "], mean: " + mean + ", outputError.length: " + outputError.length + ", input.length: " + input.length);
                }

            }

            // 3. 標準偏差の勾配を計算
            double d_stdDev = d_invStdDev * (-0.5) * Math.pow(variance + 1e-10, -1.5);

            // 4. 分散の勾配を計算
            double d_variance = d_stdDev * (1.0 / Math.sqrt(variance + 1e-10)) * (1.0 / dimension) * 2;
            for (int j = 0; j < dimension; j++) {
                d_variance *= (input[i][j] - mean);
            }


            // 5. 平均の勾配を計算
            double d_mean = 0;
            for (int j = 0; j < dimension; j++) {
                d_mean += outputError[i][j] * (-1.0 / Math.sqrt(variance + 1e-10));
                d_mean += d_variance * (-2.0 / dimension) * (input[i][j] - mean);
            }
            d_mean /= dimension;

            // 6. 入力の勾配を計算
            for (int j = 0; j < dimension; j++) {
                d_input[i][j] = outputError[i][j] * (1.0 / Math.sqrt(variance + 1e-10)) + d_variance * (2.0 / dimension) * (input[i][j] - mean) + d_mean * (1.0 / dimension);
            }
        }

        return d_input;
    }
}

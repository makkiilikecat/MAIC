package com.makkii.maic;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.makkii.maic.AIParams.*;
import static org.bukkit.Bukkit.getLogger;

public class TextGenerator {

    // プレイヤーごとのこれまでの会話内容ベクトルを保存する　double[LAYERS+1][ContextKey][次元]
    // 簡略化のために、今はnewTokensで全て上書きされる（履歴を保存しない）
    public static final HashMap<UUID, double[][][]> tokenVectorsMap = new HashMap<>();
    public static final double[][][] DEF_TOKENVECTORS = new double[LAYERS + 1][MAX_CONTEXT][DIMENSION];


    private static final double[][] tokenWordIds = new double[MAX_CONTEXT][DIMENSION];
    private static final double[] result = new double[wordSize];

    //フォワード処理
    //入力:new int[]{wordId,wordId,...};
    public static double[] generateText(List<Integer> newTokens, UUID playerUUID) {

        int contextSize = newTokens.size();
        if (contextSize < 1) return result;

        //getLogger().info("[MAIC.TextGenerator.generateText] コンテキストサイズ: " + contextSize);

        // wordId を rawVector に変換   もしかしたら別の変数にする必要はないかも
        // 最大コンテキスト数を超えてて、どこかミスってたときにエラー出させる目的でminつけてる
        for (int context = 0; context < contextSize; context++) {
            Integer token = newTokens.get(context);
            if (token == -1) continue;      // 語彙リストに無い単語の場合-1が来る
            if (context >= MAX_CONTEXT) break; // 最大コンテキスト数を超えたら無視
            System.arraycopy(embeddingMatrix[token], 0, tokenWordIds[context], 0, DIMENSION);
        }

        contextSize = Math.min(newTokens.size(), MAX_CONTEXT); // 無視したことを適用

        //getLogger().info("[MAIC.TextGenerator.generateText] TokenWordIdsへコピーしました。");


        // tokenVectors の初期化 (最初のトークンだけ)
        //本来であれば、HashMap<UUID, double[contextKey<maxContext][次元]=wordVector> tokenVectors
        //という形だが、今回は簡略化のため、historyは考えない
        //double[][][] tokenVectors = new double[LAYERS + 1][tokenWordIds.length][DIMENSION];
        double[][][] tokenVectors = tokenVectorsMap.getOrDefault(playerUUID, DEF_TOKENVECTORS.clone());

        // 入力トークンと同じサイズ　入力トークンをベクトルにしたものを保存する
        for (int context = 0; context < contextSize; context++) {
            if (DIMENSION >= 0)
                System.arraycopy(tokenWordIds[context], 0, tokenVectors[LAYERS][context], 0, DIMENSION);
        }

        //getLogger().info("[MAIC.TextGenerator.generateText] Transformerに入ります...");

        // レイヤーを重ねる
        for (int layer = 0; layer < LAYERS; layer++) {
            //getLogger().info("[MAIC.TextGenerator.generateText] レイヤー" + layer + "を処理しています...");

            // attention
            //1つ目の contextSize 次元: 出力文脈における位置を表す。つまり、出力文の何番目の単語に対応するかを示します。
            //2つ目の contextSize 次元: 入力文脈における位置を表す。つまり、入力文の何番目の単語との関連性を考慮したかを示します。
            double[][] attentionOutput = attention(tokenVectors[layer], layer);

            //getLogger().info("[MAIC.TextGenerator.generateText] Attentionが終了しました。残差接続しています...");

            // 残差接続　動画 チャプター6 15:20
            for (int i = 0; i < contextSize; i++) {
                for (int k = 0; k < DIMENSION; k++) {
                    tokenVectors[layer + 1][i][k] = attentionOutput[i][k] + tokenVectors[layer][i][k]; // += ではない
                }
            }

            //getLogger().info("[MAIC.TextGenerator.generateText] Attentionの残差接続が完了しました。MLPを処理しています,,,");

            // multiplayerPerceptron
            double[][] mppOutput = multiplayerPerceptron(tokenVectors[layer + 1], layer);

            //getLogger().info("[MAIC.TextGenerator.generateText] MLPが終了しました。残差接続しています...");

            // 残差接続 & Layer Normalization
            for (int i = 0; i < contextSize; i++) {
                for (int k = 0; k < DIMENSION; k++) {
                    tokenVectors[layer + 1][i][k] += mppOutput[i][k];
                }
            }

            //getLogger().info("[MAIC.TextGenerator.generateText] 残差接続が完了しました。");
        }

        // 掘り出し行列を使って全ての語彙の確率リストを作る
        for (int j = 0; j < DIMENSION; j++) {
            for (int k = 0; k < wordSize; k++) {
                result[k] = unEmbeddingMatrix[j][k] * tokenVectors[LAYERS][contextSize - 1][j];
            }
        }

        // 正規化する
        Utils.softMax(result);
        //getLogger().info("result: " + Arrays.toString(result));
        return result;
    }

    public static int arraysToWordId(double[] result) {
        //getLogger().info("result: " + Arrays.toString(result));

        // 簡略化のために、最も確率が高いものを選ぶ
        double max = result[0];
        int resultWordId = 0;
        for (int i = 0; i < wordSize; i++) {
            if (max < result[i]) {
                max = result[i];
                resultWordId = i;
            }
        }
        //getLogger().info("result: " + resultWordId + ", word: " + WordsLoader.getWord(resultWordId));
        return resultWordId;
    }


    private static final double[][][] queries = new double[MAX_CONTEXT][MAX_CONTEXT][DIMENSION];
    private static final double[][][] keys = new double[MAX_CONTEXT][MAX_CONTEXT][DIMENSION];
    private static final double[][][] values = new double[MAX_CONTEXT][MAX_CONTEXT][DIMENSION];
    private static final double[][] attentionWeights = new double[MAX_CONTEXT][MAX_CONTEXT];
    private static final double[][] output = new double[MAX_CONTEXT][DIMENSION]; // 修正点: [contextSize][DIMENSION] に変更

    // Scaled Dot-Product Attention
    public static double[][] attention(double[][] tokenVectors, int layer) {
        //getLogger().info("[MAIC.TextGenerator.attention] Q,K,Vを計算しています...");
        // Q, K, V の計算 (線形変換)
        // 1つ目の contextSize: 出力系列（またはクエリ）における位置を表します。つまり、「出力文の i 番目の単語」に対応します。
        // 2つ目の contextSize: 入力系列（またはキー・バリュー）における位置を表します。つまり、「入力文の j 番目の単語」に対応します。
        for (double[] recentVector : tokenVectors) {                 // コンテキスト位置
            for (int key = 0; key < MAX_CONTEXT; key++) {           // アテンション対象のコンテキスト位置
                for (int dim = 0; dim < DIMENSION; dim++) {         // コンテキストの次元
                    for (int tDim = 0; tDim < DIMENSION; tDim++) {  // アテンション対象の次元
                        // コンテキストにバイアス配列Kを掛けたもの　レイヤーを使用するのは、レイヤーによって異なる視点で見るため
                        queries[layer][key][dim] += recentVector[tDim] * attentionWeightsQ[layer][key][dim];
                        keys[layer][key][dim] += recentVector[tDim] * attentionWeightsK[layer][key][dim];
                        values[layer][key][dim] += recentVector[tDim] * attentionWeightsV[layer][key][dim];
                    }
                }
            }
        }

        //getLogger().info("[MAIC.TextGenerator.attention] query,keyのウェイトを計算しています...");

        // Attention Weights の計算
        // 出力コンテキストが入力コンテキストにどの程度注意を向けるかの指標
        for (int i = 0; i < MAX_CONTEXT; i++) {
            for (int j = 0; j < MAX_CONTEXT; j++) {
                for (int k = 0; k < DIMENSION; k++) {
                    attentionWeights[i][j] += queries[i][j][k] * keys[i][j][k];
                }
                // 動画 チャプター6 10:50
                attentionWeights[i][j] /= Math.sqrt(DIMENSION); // スケーリング
            }
        }

        // Softmax で正規化　動画 チャプター6 11:00
        Utils.softMax2D(attentionWeights);


        //getLogger().info("[MAIC.TextGenerator.attention] valueの加重平均を計算しています...");

        // 加重平均を計算
        for (int i = 0; i < MAX_CONTEXT; i++) {
            for (int k = 0; k < DIMENSION; k++) {
                for (int j = 0; j < MAX_CONTEXT; j++) { //修正点: jのループを内側に移動
                    output[i][k] += attentionWeights[i][j] * values[i][j][k]; // 修正点: output[i][k] に加算
                }
            }
        }

        return output;
    }


    private static final double[][] layer1Output = new double[MAX_CONTEXT][DIMENSION * 4];

    // Feed-Forward Network (2層の MLP)
    public static double[][] multiplayerPerceptron(double[][] tokenVector, int layer) {
        // 第1層
        for (int i = 0; i < MAX_CONTEXT; i++) {
            for (int k = 0; k < DIMENSION * 4; k++) {   // 動画 チャプター7 9:20
                for (int l = 0; l < DIMENSION; l++) {
                    layer1Output[i][k] += tokenVector[i][l] * mppWeights1[layer][l][k];
                }
                layer1Output[i][k] += mppBiases1[layer][k];  // バイアス 動画 チャプター7 8:40
                layer1Output[i][k] = Math.max(0, layer1Output[i][k]); // ReLU　動画 チャプター7 10:10
            }
        }

        // 第2層
        for (int i = 0; i < MAX_CONTEXT; i++) {
            for (int k = 0; k < DIMENSION; k++) {
                for (int l = 0; l < DIMENSION * 4; l++) {
                    output[i][k] += layer1Output[i][l] * mppWeights2[layer][l][k];   // 動画 チャプター7 11:50
                }
                output[i][k] += mppBiases2[layer][k];
            }

        }

        Utils.softMax2D(output);
        return output;
    }
}
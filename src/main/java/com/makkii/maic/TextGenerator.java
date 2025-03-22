package com.makkii.maic;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public enum TextGenerator {
    ;

    // プレイヤーごとのこれまでの会話内容ベクトルを保存する　float[LAYERS+1][ContextKey][次元]
    private static final HashMap<UUID, float[][][]> tokenVectorsMap = new HashMap<>();
    private static final float[][][] DEF_TOKENVECTORS = new float[AIParams.LAYERS + 1][AIParams.MAX_CONTEXT][AIParams.DIMENSION];

    // ai_wordsから読み取ったベクトルが入る、float[wordId][次元]の配列
    private static final float[][] rawVectors = new float[0][];


    //フォワード処理
    //入力:new int[]{wordId,wordId,...};
    public static float[][] generateText(List<Integer> newTokens, UUID playerUUID) {

        int contextSize = newTokens.size();

        // wordId を rawVector に変換   もしかしたら別の変数にする必要はないかも
        float[][] tokenWordIds = new float[contextSize][AIParams.DIMENSION];
        for (int context = 0; context < contextSize; context++) {
            Integer token = newTokens.get(context);
            System.arraycopy(rawVectors[token], 0, tokenWordIds[context], 0, AIParams.DIMENSION);
        }


        // tokenVectors の初期化 (最初のトークンだけ)
        //本来であれば、HashMap<UUID, float[contextKey<maxContext][次元]=wordVector> tokenVectors
        //という形だが、今回は簡略化のため、historyは考えない
        //float[][][] tokenVectors = new float[LAYERS + 1][tokenWordIds.length][DIMENSION];
        float[][][] tokenVectors = tokenVectorsMap.getOrDefault(playerUUID, DEF_TOKENVECTORS);

        // 入力トークンと同じサイズ　入力トークンをベクトルにしたものを保存する
        for (int context = 0; context < contextSize; context++) {
            if (AIParams.DIMENSION >= 0)
                System.arraycopy(tokenWordIds[context], 0, tokenVectors[0][context], 0, AIParams.DIMENSION);
        }

        // レイヤーを重ねる
        for (int layer = 0; layer < AIParams.LAYERS; layer++) {
            // attention
            //1つ目の contextSize 次元: 出力文脈における位置を表す。つまり、出力文の何番目の単語に対応するかを示します。
            //2つ目の contextSize 次元: 入力文脈における位置を表す。つまり、入力文の何番目の単語との関連性を考慮したかを示します。
            float[][] attentionOutput = attention(tokenVectors[layer], layer);

            // 残差接続　動画 チャプター6 15:20
            for (int i = 0; i < contextSize; i++) {
                for (int k = 0; k < AIParams.DIMENSION; k++) {
                    tokenVectors[layer + 1][i][k] = attentionOutput[i][k] + tokenVectors[layer][i][k]; // += ではない
                }
            }

            // multiplayerPerceptron
            float[][] mppOutput = multiplayerPerceptron(tokenVectors[layer + 1], layer);

            // 残差接続 & Layer Normalization (ここでは簡略化のため省略)
            for (int i = 0; i < contextSize; i++) {
                for (int k = 0; k < AIParams.DIMENSION; k++) {
                    tokenVectors[layer + 1][i][k] += mppOutput[i][k];
                }
            }

            // Layer Normalizationの追加は省略
        }

        /*ここにwordIdリストをStringにして返すメソッドが必要*/

        float[][] result = Arrays.copyOf(tokenVectors[AIParams.LAYERS], contextSize);  //最後のレイヤーをコピーして新しい配列
        tokenVectorsMap.put(playerUUID, tokenVectors);
        return result;
    }

    // Scaled Dot-Product Attention
    public static float[][] attention(float[][] tokenVectors, int layer) {
        int contextSize = tokenVectors.length;
        float[][][] queries = new float[contextSize][contextSize][AIParams.DIMENSION];
        float[][][] keys = new float[contextSize][contextSize][AIParams.DIMENSION];
        float[][][] values = new float[contextSize][contextSize][AIParams.DIMENSION];

        // Q, K, V の計算 (線形変換)
        // 1つ目の contextSize: 出力系列（またはクエリ）における位置を表します。つまり、「出力文の i 番目の単語」に対応します。
        // 2つ目の contextSize: 入力系列（またはキー・バリュー）における位置を表します。つまり、「入力文の j 番目の単語」に対応します。
        for (float[] recentVector : tokenVectors) {                 // コンテキスト位置
            for (int key = 0; key < contextSize; key++) {           // アテンション対象のコンテキスト位置
                for (int dim = 0; dim < AIParams.DIMENSION; dim++) {         // コンテキストの次元
                    for (int tDim = 0; tDim < AIParams.DIMENSION; tDim++) {  // アテンション対象の次元
                        // コンテキストにバイアス配列Kを掛けたもの　レイヤーを使用するのは、レイヤーによって異なる視点で見るため
                        queries[layer][key][dim] += recentVector[tDim] * AIParams.attentionWeightsQ[layer][key][dim];
                        keys[layer][key][dim] += recentVector[tDim] * AIParams.attentionWeightsK[layer][key][dim];
                        values[layer][key][dim] += recentVector[tDim] * AIParams.attentionWeightsV[layer][key][dim];
                    }
                }
            }
        }

        // Attention Weights の計算
        // 出力コンテキストが入力コンテキストにどの程度注意を向けるかの指標
        float[][] attentionWeights = new float[contextSize][contextSize];
        for (int i = 0; i < contextSize; i++) {
            for (int j = 0; j < contextSize; j++) {
                for (int k = 0; k < AIParams.DIMENSION; k++) {
                    attentionWeights[i][j] += queries[i][j][k] * keys[i][j][k];
                }
                // 動画 チャプター6 10:50
                attentionWeights[i][j] /= (float) 22.627416997969522; // スケーリング
            }
        }

        // Softmax で正規化　動画 チャプター6 11:00
        attentionWeights = Utils.softMax2D(attentionWeights);


        // 加重平均を計算
        float[][] output = new float[contextSize][AIParams.DIMENSION]; // 修正点: [contextSize][DIMENSION] に変更
        for (int i = 0; i < contextSize; i++) {
            for (int k = 0; k < AIParams.DIMENSION; k++) {
                for (int j = 0; j < contextSize; j++) { //修正点: jのループを内側に移動
                    output[i][k] += attentionWeights[i][j] * values[i][j][k]; // 修正点: output[i][k] に加算
                }
            }
        }

        return output;
    }

    // Feed-Forward Network (2層の MLP)
    public static float[][] multiplayerPerceptron(float[][] tokenVector, int layer) {
        int contextSize = tokenVector.length;
        float[][] output = new float[contextSize][AIParams.DIMENSION];

        // 第1層
        float[][] layer1Output = new float[contextSize][AIParams.DIMENSION * 4];
        for (int i = 0; i < contextSize; i++) {
            for (int k = 0; k < AIParams.DIMENSION * 4; k++) {   // 動画 チャプター7 9:20
                for (int l = 0; l < AIParams.DIMENSION; l++) {
                    layer1Output[i][k] += tokenVector[i][l] * AIParams.mppWeights1[layer][l][k];
                }
                layer1Output[i][k] += AIParams.mppBiases1[layer][k];  // バイアス 動画 チャプター7 8:40
                layer1Output[i][k] = Math.max(0, layer1Output[i][k]); // ReLU　動画 チャプター7 10:10
            }
        }

        // 第2層
        for (int i = 0; i < contextSize; i++) {
            for (int k = 0; k < AIParams.DIMENSION; k++) {
                for (int l = 0; l < AIParams.DIMENSION * 4; l++) {
                    output[i][k] += layer1Output[i][l] * AIParams.mppWeights2[layer][l][k];   // 動画 チャプター7 11:50
                }
                output[i][k] += AIParams.mppBiases2[layer][k];
            }

        }
        return output;
    }
}
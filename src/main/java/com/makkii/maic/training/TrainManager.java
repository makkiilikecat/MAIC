package com.makkii.maic.training;

import com.makkii.maic.TextGenerator;
import com.makkii.maic.Utils;
import com.makkii.maic.file_manager.WordsFileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TrainManager {

    // 定数
    public static final UUID TRAINERUUID = UUID.randomUUID();

    public static final int DIMENSION = 512; // ベクトルの次元数
    public static final int MAX_CONTEXT = 256; // 最大コンテキスト長
    public static final double LEARNING_RATE = 0.001; // 学習率
    public static final int BATCH_SIZE = 32; // バッチサイズ
    public static final int EPOCHS = 10; // エポック数
    public static final String AI_WORDS_FILE_NAME = "ai_words.txt";

    // 語彙とそのID (ai_words に相当)
    public static List<String> vocabulary = new ArrayList<>();
    public static Map<String, Integer> wordToId = new HashMap<>();

    // 埋め込み行列 (ai_vectors に相当)
    public static float[][] embeddingMatrix;

    // attention 用の重み行列
    public static final int K_Q_DIMENTION = 32;
    public static float[][][] attentionWeightsQ; // [contextKey][DIMENSION]
    public static float[][][] attentionWeightsK; // [contextKey][DIMENSION]
    public static float[][][] attentionWeightsV; // [contextKey][DIMENSION]

    //playerPerceptron 用の重み行列とバイアス
    public static float[][][] mppWeights1; // [layer][DIMENSION][DIMENSION * 4]
    public static float[][] mppBiases1;  // [layer][DIMENSION * 4]
    public static float[][][] mppWeights2; // [layer][DIMENSION * 4][DIMENSION]
    public static float[][] mppBiases2;  // [layer][DIMENSION]

    public static final int LAYERS = 6; // レイヤー数


    public static void main() {
        // 1. データの準備 (ここではサンプルデータを直接コード内に記述)
        //prepareData();
        callWordsFileManager();

        // 2. モデルの初期化
        Initializer.initializeModel();

        // 3. 学習ループ
        train();
    }

    private static void callWordsFileManager() {
        try {
            // ai_words.txt が存在しない場合
            WordsFileManager.createWordsFileFromDatabase("path/to/ai_database.txt", AI_WORDS_FILE_NAME);

            // ai_words.txt が存在する場合
            WordsFileManager.loadWordsFile(AI_WORDS_FILE_NAME);

            vocabulary = Arrays.asList(WordsFileManager.getVocabulary());
            wordToId = WordsFileManager.getWordToId();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void prepareData() {
        // サンプルデータ (Wikipedia の一部と仮定)
        String rawText = "これは自然言語処理のテストです。 大規模言語モデルは非常に強力です。";

        // 文字単位でトークン化 (簡単のため)
        for (char c : rawText.toCharArray()) {
            if (!wordToId.containsKey(String.valueOf(c))) {
                vocabulary.add(String.valueOf(c));
                wordToId.put(String.valueOf(c), vocabulary.size() - 1);
            }
        }
    }


    public static void train() {
        Random random = new Random();
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            System.out.println("Epoch: " + (epoch + 1));
            //データをシャッフルする
            Collections.shuffle(vocabulary, random);

            // バッチ処理
            for (int i = 0; i < vocabulary.size() - BATCH_SIZE; i += BATCH_SIZE) {

                //フォワード処理、損失計算、バックプロパゲーションを行う
                final List<Integer> batchWordIds = new ArrayList<>();
                for (int j = 0; j < BATCH_SIZE; j++) {
                    batchWordIds.add(wordToId.get(vocabulary.get(i + j)));
                }

                trainStep(batchWordIds);
            }
        }
    }

    public static void trainStep(final List<Integer> batchWordIds) {
        float[][] output = TextGenerator.generateText(batchWordIds, TRAINERUUID);
        // 4. 損失関数の計算 (ここでは簡略化のため、具体的な損失計算は省略)
        final float loss = Utils.calculateLoss(output, batchWordIds);

        // 5. バックプロパゲーション (各パラメータの勾配を計算)
        // ... (ここに、各パラメータの偏微分を計算するコードを記述)
        // 例:
        final float[][] gradients = Utils.calculateGradients(loss, output, batchWordIds);
        // 6. パラメータの更新
        Utils.updateParameters(gradients);
    }
}
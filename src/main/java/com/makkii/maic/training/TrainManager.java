package com.makkii.maic.training;

import com.makkii.maic.TextGenerator;
import com.makkii.maic.Utils;
import com.makkii.maic.file_manager.words.WordsLoader;

import java.io.IOException;
import java.util.*;

import static com.makkii.maic.AIParams.*;
import static com.makkii.maic.file_manager.FileManager.AI_WORDS_FILE_NAME;
import static org.bukkit.Bukkit.getLogger;

public enum TrainManager {
    ;

    public static final UUID TRAINERUUID = UUID.randomUUID();

    public static void main() {

        // ai_words.txtをロードする
        try {
            getLogger().info("[MAIC] " + AI_WORDS_FILE_NAME + "をロードしています...");

            long startTime = System.currentTimeMillis();
            WordsLoader.loadWords();
            getLogger().info("[MAIC: ロード時間: " + (System.currentTimeMillis() - startTime) + "ms");

            getLogger().info("[MAIC] 語彙サイズ: " + wordSize);

            startTime = System.nanoTime();
            for (int i = 0; i < wordSize; i++) {
                WordsLoader.getIndex(WordsLoader.getWord(i));
            }
            getLogger().info("[MAIC] wordIdからwordからwordId: " + (System.nanoTime() - startTime) / 1000 / 1000 + "ms");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. モデルの初期化
        Initializer.initializeModel();

        // 3. 学習ループ
        //train();
    }


//    public static void train() {
//        Random random = new Random();
//
//        for (int epoch = 0; epoch < EPOCHS; epoch++) {
//            System.out.println("Epoch: " + (epoch + 1));
//            //データをシャッフルする
//            Collections.shuffle(vocabulary, random);
//
//            // バッチ処理
//            for (int i = 0; i < vocabulary.size() - BATCH_SIZE; i += BATCH_SIZE) {
//
//                //フォワード処理、損失計算、バックプロパゲーションを行う
//                List<Integer> batchWordIds = new ArrayList<>();
//                for (int j = 0; j < BATCH_SIZE; j++) {
//                    batchWordIds.add(wordToId.get(vocabulary.get(i + j)));
//                }
//
//                trainStep(batchWordIds);
//            }
//        }
//    }
//
//    public static void trainStep(List<Integer> batchWordIds) {
//        float[][] output = TextGenerator.generateText(batchWordIds, TRAINERUUID);
//        // 4. 損失関数の計算 (ここでは簡略化のため、具体的な損失計算は省略)
//        float loss = Utils.calculateLoss(output, batchWordIds);
//
//        // 5. バックプロパゲーション (各パラメータの勾配を計算)
//        // ... (ここに、各パラメータの偏微分を計算するコードを記述)
//        // 例:
//        float[][] gradients = Utils.calculateGradients(loss, output, batchWordIds);
//        // 6. パラメータの更新
//        Utils.updateParameters(gradients);
//    }
}
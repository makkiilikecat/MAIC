package com.makkii.maic.training;

import com.makkii.maic.TextGenerator;
import com.makkii.maic.file_manager.BaseDataLoader;
import com.makkii.maic.file_manager.SentenceLabeler;
import com.makkii.maic.file_manager.words.WordsLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.makkii.maic.AIParams.*;
import static com.makkii.maic.file_manager.FileManager.AI_BASEDATA_FILE_NAME;
import static com.makkii.maic.file_manager.FileManager.AI_WORDS_FILE_NAME;
import static org.bukkit.Bukkit.getLogger;

public class TrainManager {

    public static final UUID TRAINERUUID = UUID.randomUUID();
    static int numThreads = 1;//Runtime.getRuntime().availableProcessors() / 2;


    public static void main() {

        // ai_words.txtをロードする
        try {
            getLogger().info("[MAIC] " + AI_WORDS_FILE_NAME + "をロードしています...");

            long startTime = System.currentTimeMillis();
            WordsLoader.loadWords();
            getLogger().info("[MAIC: ロード時間: " + (System.currentTimeMillis() - startTime) + "ms");

            getLogger().info("[MAIC] 語彙サイズ: " + wordSize);

            startTime = System.nanoTime();
            int nullCount = 0;
            for (int i = 0; i < wordSize; i++) {
                int wordId = WordsLoader.getIndex(WordsLoader.getWord(i));
                if (wordId == -1) nullCount++;
            }
            getLogger().info("[MAIC] wordIdからwordからwordId: " + (System.nanoTime() - startTime) / 1000 / 1000 + "ms｜nullだった語彙数: " + nullCount);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. モデルの初期化
        Initializer.initializeModel();

        // 3. 学習ループ
        train();
    }

    private static void train() {

        getLogger().info("[MAIC Trainer] " + AI_BASEDATA_FILE_NAME + "から正解ラベルを作成しています...");

        List<String> sentenceList = SentenceLabeler.basedataSplit();
        //MAX_CONTEXTより大きい文、5より小さい文を削除
        sentenceList.removeIf(sentence -> sentence.length() < 5 || sentence.length() > MAX_CONTEXT);

        getLogger().info("[MAIC Trainer] 正解ラベルが作成できました。学習ループを開始します。正解ラベルのサイズ: " + sentenceList.size());
        getLogger().info("[MAIC Trainer] 使用するスレッド数: " + numThreads);


        int completecount = 0;
        int nancount = 0;
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            getLogger().info("--------------------------------------------------");
            getLogger().info("Epoch " + (epoch + 1) + " / " + EPOCHS + " 開始");

            // データをシャッフル
            //Collections.shuffle(sentenceList);
            getLogger().info("Epoch " + (epoch + 1) + " / " + EPOCHS + " データシャッフル完了");

            // ミニバッチの作成
            List<List<Integer>> miniBatches = BaseDataLoader.createMiniBatches(sentenceList);
            getLogger().info("Epoch " + (epoch + 1) + " / " + EPOCHS + " ミニバッチ作成完了. バッチ数: " + miniBatches.size());

            double totalLoss = 0;
            int batchCount = 0;
            int totalBatches = miniBatches.size();

            double lastAvgLoss = 0;
            for (List<Integer> miniBatch : miniBatches) {
                //getLogger().info(Arrays.toString(WordsLoader.convertToWordList(miniBatch).toArray()));
                if (batchCount % 1 == 0 && batchCount != 0) {
                    getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 完了. 平均損失: " + totalLoss / completecount);
                    getLogger().info("nancount/completecount: " + nancount + "/" + completecount + ", 前回との差: " + (totalLoss / completecount - lastAvgLoss));
                    lastAvgLoss = totalLoss / completecount;
                }
                batchCount++;

                // 順伝播
                double[] predictedProbabilities = TextGenerator.generateText(miniBatch, TRAINERUUID);
                //if (batchCount % 1== 0) getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 順伝播完了");

                // 正解ラベルの取得 (ミニバッチの最後のトークンを正解とする)
                int targetIndex = miniBatch.get(miniBatch.size() - 1);

                getLogger().info("推測された単語: " + WordsLoader.getWord(TextGenerator.arraysToWordId(predictedProbabilities)) + ", 正解単語: " + WordsLoader.getWord(targetIndex));

                // 損失の計算
                double loss = LossCalculator.calculateCrossEntropyLoss(predictedProbabilities, targetIndex);
                if (Double.isNaN(loss)) {
                    nancount++;
                    //getLogger().info("loss: " + loss);
                    //getLogger().info("predictedProbabiliteies: " +
                    //        Arrays.toString(Arrays.stream(predictedProbabilities).toArray()) + ", targetIndex: " + targetIndex);
                    //getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 損失がNaNになりました。バッチをスキップします。");
                    continue;
                } else {
                    totalLoss += loss;
                    completecount++;
                    //getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 損失計算完了: " + loss);
                }

                // 逆伝播
                Backpropagation.calculateGradients(predictedProbabilities, targetIndex, miniBatch, TRAINERUUID);
                //getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 逆伝播完了");

                // パラメータの更新
                Optimizer.updateParameters(LEARNING_RATE);
                Optimizer.resetGradients();
            }
            getLogger().info("Epoch " + (epoch + 1) + ", Batch " + batchCount + " / " + totalBatches + " 完了. 平均損失: " + totalLoss / completecount);

            // エポックごとの平均損失を表示
            getLogger().info("--------------------------------------------------");
            getLogger().info("Epoch " + (epoch + 1) + " / " + EPOCHS + " 完了. 平均損失: " + totalLoss / completecount);
            getLogger().info("nancount/completecount: " + nancount + "/" + completecount);
        }
        getLogger().info("学習完了!");
    }
}
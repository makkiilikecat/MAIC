package com.makkii.maic.training;

import com.makkii.maic.AIParams;
import com.makkii.maic.KuromojiTokenizer;
import com.makkii.maic.TextGenerator;
import com.makkii.maic.Utils;
import com.makkii.maic.file_manager.SentenceLabeler;
import com.makkii.maic.file_manager.words.WordsLoader;

import java.io.IOException;
import java.util.*;

import static com.makkii.maic.AIParams.*;
import static com.makkii.maic.file_manager.FileManager.AI_BASEDATA_FILE_NAME;
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

        List<String> sentenceList = SentenceLabeler.splitIntoSentences();
        for (int i = 0; i < sentenceList.size(); i++) {
            int length = sentenceList.get(i).length();
            if (length < 5 || length > MAX_CONTEXT) {
                sentenceList.remove(i);
            }
        }

        getLogger().info("[MAIC Trainer] 正解ラベルが作成できました。学習ループを開始します。正解ラベルのサイズ: " + sentenceList.size());

        int count = 0;
        int sentenceLength = sentenceList.size();
        for (String sentence : sentenceList) {
            count++;
            if (count % 100 == 0)
                getLogger().info(new StringBuilder("[MAIC Trainer] 学習進捗: ").append(count).append("/").append(sentenceLength).toString());

            // 区切られた文をトークン化する
            ArrayList<String> tokenizedSentence = KuromojiTokenizer.tokenize(sentence);

            //getLogger().info("tokenizedSentence: " + tokenizedSentence);

            // トークンから語居idを取得
            List<Integer> targetTokenIds = new ArrayList<>();
            for (int i = 0; i < tokenizedSentence.size(); i++) {
                int wordId = WordsLoader.getIndex(tokenizedSentence.get(i));
                if (wordId == -1) continue;
                targetTokenIds.add(wordId);
            }
            // 生成
            int predictedWordId = TextGenerator.generateText(targetTokenIds, TRAINERUUID);
            String word = WordsLoader.getWord(predictedWordId);

            //getLogger().info("[MAIC] predictedWordId: " + predictedWordId + ", word: " + word);
            //getLogger().info("[MAIC] sentence: " + sentence + word);

            // 損失計算
            //float loss = LossCalculator.calculateLossWithSoftmax(predictedWordId, targetTokenIds);

            //getLogger().info("[MAIC] loss: " +
            //break;
        }
    }
}
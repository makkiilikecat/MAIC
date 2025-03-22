package com.makkii.maic.oldAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.bukkit.Bukkit.getLogger;

public class NGramModel {

    private final int n; // n-gramのn
    private final DataLoader dataLoader;

    public NGramModel(int n, DataLoader dataLoader) {
        this.n = n;
        this.dataLoader = dataLoader;
    }

    public int getN() {
        return n;
    }

    public String predictNextWord(List<String> context) {
        //コンテキストのサイズ調整
        if (context.size() > n - 1) {
            context = context.subList(context.size() - (n - 1), context.size());
        }

        // 文脈のベクトルを計算（単純に平均を取る）
        double[] contextVector = calculateContextVector(context);

        if (contextVector == null) {
            // contextWordsの単語が全て辞書にない場合、適当な単語を返す
            Set<String> words = dataLoader.getWords();
            //getLogger().info("words: " + words);
            if (!words.isEmpty()) {
                // wordsが空でなければランダムに選択
                return words.toArray(new String[0])[new Random().nextInt(words.size())];
            }
            return "[しらない単語]";
        }

        // 全単語との類似度を計算し、最も類似度が高い単語を選ぶ
        String bestWord = null;
        double bestSimilarity = -1.0; // コサイン類似度は-1から1の範囲なので、-1で初期化

        for (String word : dataLoader.getWords()) {
            double[] wordVector = dataLoader.getWordVector(word);
            if (wordVector == null) {
                getLogger().info("skip");
                continue; // ベクトルがない単語はスキップ
            }
            double similarity = Util.cosineSimilarity(contextVector, wordVector);

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestWord = word;
            }
        }

        if (bestWord == null) {
            return "[bestWord null]"; // 見つからない場合は特殊なトークンを返す
        } else {
            return bestWord;
        }
    }

    private double[] calculateContextVector(List<String> context) {
        List<double[]> vectors = new ArrayList<>();
        for (String word : context) {
            double[] vector = dataLoader.getWordVector(word);
            if (vector != null) {
                vectors.add(vector);
            }
        }

        if (vectors.isEmpty()) {
            return null; // ベクトルが見つからない場合はnullを返す
        }

        // ベクトルの平均を計算
        double[] averageVector = new double[vectors.get(0).length];
        for (double[] vector : vectors) {
            for (int i = 0; i < vector.length; i++) {
                averageVector[i] += vector[i];
            }
        }
        for (int i = 0; i < averageVector.length; i++) {
            averageVector[i] /= vectors.size();
        }
        return averageVector;
    }

    // 以前の頻度ベースのメソッドは削除
    // private double calculateProbability(...) { ... }
    // private int getTotalCount(...) { ... }
    // private String extractContext(...) { ... }
}
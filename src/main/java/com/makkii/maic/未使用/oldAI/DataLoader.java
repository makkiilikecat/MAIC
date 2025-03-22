package com.makkii.maic.oldAI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataLoader {

    private final Map<String, double[]> wordVectors; // 単語とベクトルのマッピング

    public DataLoader() {
        wordVectors = new HashMap<>();
    }

    public void loadData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length < 2) continue; // 形式が不正な行はスキップ

                String word = parts[0];
                // ベクトル部分の文字列からdouble[]配列に
                double[] vector = new double[parts.length - 1];
                for (int j = 1; j < parts.length; j++) {
                    vector[j - 1] = Double.parseDouble(parts[j]);
                }
                //if ((i % 100000) == 0) getLogger().info("word: " + word + ", vector: " + vector);
                wordVectors.put(word, vector);
                i++;
            }
        } catch (IOException | NumberFormatException e) {
            //TODO: エラーハンドリング（ログ出力、例外処理など）
            e.printStackTrace();
        }
    }

    public double[] getWordVector(String word) {
        return wordVectors.get(word); // 存在しない場合はnullを返す
    }

    // 他のクラスで単語のリストが必要になった場合のために追加
    public Set<String> getWords() {
        return wordVectors.keySet();
    }

    //直接頻度情報を持たなくなったので削除
    //public Map<String, Integer> getNGramFrequency() {
    //    return this.ngramFrequency;
    //}
}
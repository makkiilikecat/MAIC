package com.makkii.maic.oldAI;

import java.util.Arrays;
import java.util.List;

public class Util {
    // ... 既存の preprocess, tokenize メソッド ...
    public static String preprocess(String text) {
        // TODO: 小文字化
        text = text.toLowerCase();

        // TODO: 不要な記号の削除（正規表現などを利用）
        text = text.replaceAll("[^a-zA-Z0-9\\s]", ""); // 英数字と空白以外を削除

        // TODO: その他の前処理（必要に応じて）

        return text;
    }

    public static List<String> tokenize(String text) {
        // TODO: 形態素解析器の利用を検討（MeCab, Kuromojiなど）
        // とりあえず、単純な空白区切りでトークン化
        return Arrays.asList(text.split("\\s+"));
    }

    /**
     * 二つのベクトルの内積を計算する
     *
     * @param vec1
     * @param vec2
     * @return 内積
     */
    public static double dotProduct(double[] vec1, double[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            throw new IllegalArgumentException("Invalid vectors for dot product.");
        }
        double product = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            product += vec1[i] * vec2[i];
        }
        return product;
    }

    /**
     * cosine類似度を計算
     */
    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < Math.min(vectorA.length, vectorB.length); i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
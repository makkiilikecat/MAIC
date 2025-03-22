package com.makkii.maic.file_manager;

import lombok.Getter;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum WordsFileManager {
    ;

    private static final Pattern IGNORE_TEXT_PATTERN = Pattern.compile("[\"：:()\\[\\]「」、。]");
    @Getter
    private static String[] vocabulary;
    @Getter
    private static Map<String, Integer> wordToId;

    // 1. ai_database ファイルの読み込み
    public static String loadDatabase(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // 2. テキストの前処理
    public static String preprocessText(String text) {
        // 不要な文字を除去 (ダブルクオーテーション, コロン, かっこ, 句読点)
        String processedText = IGNORE_TEXT_PATTERN.matcher(text).replaceAll("");
        // 必要に応じて小文字に統一 (オプション)
        // processedText = processedText.toLowerCase();
        return processedText;
    }

    // 3. トークン化 (文字単位)
    public static List<String> tokenize(String text) {
        // 文字単位で分割し、重複を排除
        return text.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .distinct()
                .collect(Collectors.toList());
    }

    // 4. ai_words.txt の生成
    public static void createWordsFile(List<String> tokens, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            String content = String.join(",", tokens); // カンマ区切りで結合
            writer.write(content);
        }
    }

    // 5. 語彙の管理
    public static void buildVocabulary(List<String> tokens) {
        vocabulary = tokens.toArray(new String[0]); // リストを配列に変換
        wordToId = new HashMap<>();
        for (int i = 0; i < vocabulary.length; i++) {
            wordToId.put(vocabulary[i], i); // トークンとインデックス(ID)を関連付け
        }
    }

    // (オプション) 頻度によるフィルタリング
    public static List<String> filterByFrequency(List<String> tokens, String text, int minFrequency) {
        Map<String, Long> frequencyMap = text.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        return tokens.stream()
                .filter(token -> frequencyMap.getOrDefault(token, 0L) >= minFrequency)
                .collect(Collectors.toList());
    }

    // (オプション) 特殊トークンの追加
    public static List<String> addSpecialTokens(List<String> tokens) {
        List<String> newTokens = new ArrayList<>();
        newTokens.add("<unk>"); // 未知語
        newTokens.add("<s>");   // 文頭
        newTokens.add("</s>");  // 文末
        newTokens.addAll(tokens);
        return newTokens;
    }

    // メイン処理 (外部から呼び出すメソッド)
    public static void createWordsFileFromDatabase(String databasePath, String outputPath) throws IOException {
        createWordsFileFromDatabase(databasePath, outputPath, 0, false);
    }

    public static void createWordsFileFromDatabase(String databasePath, String outputPath, int minFrequency) throws IOException {
        createWordsFileFromDatabase(databasePath, outputPath, minFrequency, false);
    }

    public static void createWordsFileFromDatabase(String databasePath, String outputPath, boolean useSpecialTokens) throws IOException {
        createWordsFileFromDatabase(databasePath, outputPath, 0, useSpecialTokens);
    }

    public static void createWordsFileFromDatabase(String databasePath, String outputPath, int minFrequency, boolean useSpecialTokens) throws IOException {
        String rawText = loadDatabase(databasePath);
        String processedText = preprocessText(rawText);
        List<String> tokens = tokenize(processedText);

        if (minFrequency > 0) {
            tokens = filterByFrequency(tokens, processedText, minFrequency);
        }
        if (useSpecialTokens) {
            tokens = addSpecialTokens(tokens);
        }

        createWordsFile(tokens, outputPath);
        buildVocabulary(tokens);
    }

    //WordsFileManagerをTrainManagerクラスで使うためのメソッド
    public static void loadWordsFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); //1行しかないことを想定
            if (line != null) {
                List<String> tokens = Arrays.asList(line.split(","));
                buildVocabulary(tokens);
            }

        }
    }
}
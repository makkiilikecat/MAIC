package com.makkii.maic.oldAI.newAI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getLogger;

public class BPE {

    public static final String AI_DATABASE_FILE = "ai_database";
    public static final String AI_DATABASE_JSON_FILE = "ai_database.json";
    public static final String WORDS_JSON_FILE = "words.json";
    public static final int VOCAB_SIZE = 1000;

    private final String configDir;

    public BPE(String configDir) {
        this.configDir = configDir;
    }

    // "ai_database"ファイルから"ai_database.json"を生成するメソッド
    public void createAiDatabaseJson() throws IOException {
        File jsonFile = new File(configDir, AI_DATABASE_JSON_FILE);
        if (jsonFile.exists()) {
            return; // 既に存在する場合は処理をスキップ
        }

        File databaseFile = new File(configDir, AI_DATABASE_FILE);
        if (!databaseFile.exists()) {
            getLogger().info(AI_DATABASE_FILE + "が存在しません。AI関連の機能は全て無効になります。");
            return;
        }

        getLogger().info("[MAIC] " + AI_DATABASE_JSON_FILE + "を作成中...");

        //JSONArray jsonArray = new JSONArray();
        Pattern docPattern = Pattern.compile("<doc id=\"(\\d+)\" url=\"(.*?)\" title=\"(.*?)\">");

        // try-with-resources で reader と writer を自動的に閉じる
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(databaseFile.toPath()), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(jsonFile.toPath()), StandardCharsets.UTF_8)))
        {

            writer.write("["); // JSON配列の開始

            String line;
            StringBuilder content = new StringBuilder();
            String currentId = null;
            String currentUrl = null;
            String currentTitle = null;
            boolean firstDocument = true;

            while ((line = reader.readLine()) != null) {
                Matcher docMatcher = docPattern.matcher(line);
                if (docMatcher.find()) {
                    // 新しいドキュメントの開始
                    if (currentId != null) {
                        // 既存のドキュメントをJSONに書き込み (メモリ節約のため、部分的に書き出す)
                        if (!firstDocument) {
                            writer.write(","); // ドキュメント間の区切り
                        }
                        firstDocument = false;
                        addDocumentAndWriteToJson(writer, currentId, currentUrl, currentTitle, content.toString());
                        content.setLength(0); // StringBuilderをクリア
                    }
                    // 新しいドキュメントの情報をセット
                    currentId = docMatcher.group(1);
                    currentUrl = docMatcher.group(2);
                    currentTitle = docMatcher.group(3);
                    content = new StringBuilder(); // 新しいStringBuilderインスタンスを作成
                } else {
                    // ドキュメントの内容を追加
                    content.append(line).append("\n");
                }
            }
            // 最後のドキュメントを書き込み
            if (currentId != null) {
                if (!firstDocument) {
                    writer.write(",");
                }
                addDocumentAndWriteToJson(writer, currentId, currentUrl, currentTitle, content.toString());
            }

            writer.write("]"); // JSON配列の終了
        }

        getLogger().info("[MAIC] " + AI_DATABASE_JSON_FILE + "の作成を完了しました。");
    }

    // JSONオブジェクトを作成し、部分的にファイルに書き込むヘルパーメソッド
    private void addDocumentAndWriteToJson(BufferedWriter writer, String id, String url, String title, String content) throws IOException {
        // HTMLタグの削除
        content = content.replaceAll("<[^>]*>", "");

        // エスケープ処理
        content = escapeJsonString(content);
        title = escapeJsonString(title);
        url = escapeJsonString(url);

        JSONObject docObject = new JSONObject();
        docObject.put("id", id);
        docObject.put("url", url);
        docObject.put("title", title);
        docObject.put("text", content);

        writer.write(docObject.toString(2)); // インデント付きで書き込み (部分書き込み)
    }

    // "ai_database.json"から"words.json"を生成するメソッド
    public void createWordsJson() throws IOException {
        File wordsJsonFile = new File(configDir, WORDS_JSON_FILE);
        if (wordsJsonFile.exists()) {
            return; // 既に存在する場合は処理をスキップ
        }

        File jsonFile = new File(configDir, AI_DATABASE_JSON_FILE);
        if (!jsonFile.exists()) {
            throw new FileNotFoundException(AI_DATABASE_JSON_FILE + " not found in " + configDir);
        }

        long militime = System.currentTimeMillis();
        getLogger().info(WORDS_JSON_FILE + " から " + AI_DATABASE_JSON_FILE + "を作成しています...");

        // JSONファイルの読み込み
        StringBuilder allText = new StringBuilder();

        // try-with-resources で reader を自動的に閉じる
        int skippedLines = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(jsonFile.toPath()), StandardCharsets.UTF_8))) {
            String line;
            JSONObject jsonObject;

            // 1行ずつ読み込み、JSONObjectに変換してテキストを抽出
            while ((line = reader.readLine()) != null) {
                if (line.equals("[") || line.equals("]")) {
                    continue;
                }
                //最後の行の処理
                if (line.endsWith("]")) {
                    line = line.substring(0, line.length() - 1);
                }
                //行の途中にコンマがある場合の処理
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
                try {
                    jsonObject = new JSONObject(line); // 1行をJSONObjectに変換
                    allText.append(jsonObject.getString("text")); // "text"キーの内容を抽出
                } catch (Exception e) {
                    // JSONパースエラーの処理 (必要に応じて)
                    skippedLines++;
                    //getLogger().warning("Skipping invalid JSON line: ");
                    continue; // 問題がある行はスキップ
                }
            }
        }

        getLogger().info("1/2: テキストの結合が完了しました。" + skippedLines + "行をエラーのためスキップしました。（" + (System.currentTimeMillis() - militime) / 1000 + "秒）");
        militime = System.currentTimeMillis();

        // BPEアルゴリズムの実行
        JSONObject wordsJson = trainBPE(allText.toString(), VOCAB_SIZE);

        getLogger().info("2/2: BPEアルゴリズムの処理が完了しました。（" + (System.currentTimeMillis() - militime) / 1000 + "秒）");
        militime = System.currentTimeMillis();
        getLogger().info("words.jsonを作成中...");

        // JSONファイルに書き込み
        try (FileWriter fileWriter = new FileWriter(wordsJsonFile)) {
            fileWriter.write(wordsJson.toString(2)); // インデント付きで書き込み
        }

        getLogger().info(WORDS_JSON_FILE + " の作成が完了しました。（" + (System.currentTimeMillis() - militime) / 1000 + "秒）");
    }

    // JSONオブジェクトをJSONArrayに追加するヘルパーメソッド
    private void addDocumentToJson(JSONArray jsonArray, String id, String url, String title, String content) {
        // HTMLタグの削除
        content = content.replaceAll("<[^>]*>", "");

        // エスケープ処理 (JSONで特別な意味を持つ文字をエスケープ)
        content = escapeJsonString(content);
        title = escapeJsonString(title);
        url = escapeJsonString(url);

        JSONObject docObject = new JSONObject();
        docObject.put("id", id);
        docObject.put("url", url);
        docObject.put("title", title);
        docObject.put("text", content);
        jsonArray.put(docObject);
    }

    // JSON文字列のエスケープ処理
    private String escapeJsonString(String text) {
        return text.replace("\\", "\\\\")  // バックスラッシュ
                .replace("\"", "\\\"")  // 二重引用符
                .replace("\b", "\\b")  // バックスペース
                .replace("\f", "\\f")  // フォームフィード
                .replace("\n", "\\n")  // 改行
                .replace("\r", "\\r")  // 復帰
                .replace("\t", "\\t"); // タブ
    }

    // BPEアルゴリズムの実装
    private JSONObject trainBPE(String text, int vocabSize) {
        // 1. 初期語彙の作成 (文字単位)
        Set<String> initialVocab = text.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.toSet());

        HashMap<String, Integer> vocab = new HashMap<>();
        int idCounter = 0;
        for (String symbol : initialVocab) {
            // 既にvocabに存在するかどうかを確認
            if (!vocab.containsKey(symbol)) {
                vocab.put(symbol, idCounter++);
            }
        }

        // 2. 単語分割 (初期は文字分割)
        List<String> words = new ArrayList<>();
        Pattern wordPattern = Pattern.compile("\\b\\w+\\b"); // 単語のパターン (必要に応じて調整)
        Matcher wordMatcher = wordPattern.matcher(text);
        while (wordMatcher.find()) {
            words.add(wordMatcher.group());
        }
        List<String> splitWords = new ArrayList<>();
        for (String word : words) {
            splitWords.addAll(Arrays.asList(word.split("")));
        }


        // 3. 頻度計算
        Map<Pair<String, String>, Integer> pairFreqs = getPairFreqs(splitWords);

        //マージのリスト
        List<List<String>> merges = new ArrayList<>();

        // 4. BPEメインループ
        int count = 0;
        while (vocab.size() < vocabSize) {
            count++;
            if (0 == 0) getLogger().info("[BPE] 進捗: " + count + "/ " + vocabSize + ")");

            Pair<String, String> bestPair = pairFreqs.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (bestPair == null) {
                getLogger().info("[BPE] マージペアがないため終了します。");
                break; // マージするペアがない場合は終了
            }

            // 新しいシンボルの作成
            String newSymbol = bestPair.getFirst() + bestPair.getSecond();
            // 既にvocabに存在するかどうかを確認
            //if (!vocab.containsKey(newSymbol)) {
            vocab.put(newSymbol, idCounter++);
            merges.add(Arrays.asList(bestPair.getFirst(), bestPair.getSecond()));
            //}

            // 単語リストの更新
            splitWords = mergePairs(splitWords, bestPair, newSymbol);

            // 頻度情報の更新
            pairFreqs = getPairFreqs(splitWords);
        }

        // 5. 結果の整形
        JSONObject result = new JSONObject();
        JSONObject vocabJson = new JSONObject(vocab);
        result.put("vocab", vocabJson);
        result.put("merges", merges);
        return result;
    }

    // ペアの頻度を計算するメソッド
    private Map<Pair<String, String>, Integer> getPairFreqs(List<String> words) {
        Map<Pair<String, String>, Integer> pairFreqs = new HashMap<>();
        for (int i = 0; i < words.size() - 1; i++) {
            Pair<String, String> pair = new Pair<>(words.get(i), words.get(i + 1));
            pairFreqs.put(pair, pairFreqs.getOrDefault(pair, 0) + 1);
        }
        return pairFreqs;
    }

    // 指定されたペアをマージするメソッド
    private List<String> mergePairs(List<String> words, Pair<String, String> pair, String newSymbol) {
        List<String> newWords = new ArrayList<>();
        int i = 0;
        while (i < words.size()) {
            if (i < words.size() - 1 && words.get(i).equals(pair.getFirst()) && words.get(i + 1).equals(pair.getSecond())) {
                newWords.add(newSymbol);
                i += 2;
            } else {
                newWords.add(words.get(i));
                i++;
            }
        }
        return newWords;
    }

    //文字列ペアを格納する内部クラス
    public static class Pair<T1, T2> {
        private final T1 first;
        private final T2 second;

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }

        public T1 getFirst() {
            return first;
        }

        public T2 getSecond() {
            return second;
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) obj;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }
    }

    // words.json を読み込んでトークナイズを行う
    public List<Integer> tokenize(String text) {
        // 1. words.jsonの読み込み
        File wordsJsonFile;
        String jsonContent;
        JSONObject wordsJson;
        JSONObject vocabJson;
        JSONArray mergesJson;
        try {
            wordsJsonFile = new File(configDir, WORDS_JSON_FILE);
            jsonContent = new String(Files.readAllBytes(Paths.get(wordsJsonFile.getAbsolutePath())), StandardCharsets.UTF_8);
            wordsJson = new JSONObject(jsonContent);
            vocabJson = wordsJson.getJSONObject("vocab");
            mergesJson = wordsJson.getJSONArray("merges");
        } catch (IOException e) {
            getLogger().warning("Failed to load words.json.");
            return new ArrayList<>();
        }

        Map<String, Integer> vocab = new HashMap<>();
        Iterator<String> keys = vocabJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            vocab.put(key, vocabJson.getInt(key));
        }

        // 2. 単語分割
        List<String> words = new ArrayList<>();
        Pattern wordPattern = Pattern.compile("\\b\\w+\\b"); // 単語のパターン (必要に応じて調整)
        Matcher wordMatcher = wordPattern.matcher(text);
        while (wordMatcher.find()) {
            words.add(wordMatcher.group());
        }
        List<String> splitWords = new ArrayList<>();
        for (String word : words) {
            splitWords.addAll(Arrays.asList(word.split("")));
        }

        //getLogger().info("BPE.tokenize.splitWords: " + splitWords);

        // 3. マージの実行
        for (int i = 0; i < mergesJson.length(); i++) {
            JSONArray merge = mergesJson.getJSONArray(i);
            String first = merge.getString(0);
            String second = merge.getString(1);
            Pair<String, String> pair = new Pair<>(first, second);
            String newSymbol = first + second;
            splitWords = mergePairs(splitWords, pair, newSymbol);
        }

        //getLogger().info("BPE.tokenize.marged_splitWords: " + splitWords);

        // 4. トークンIDへの変換
        List<Integer> tokens = new ArrayList<>();
        for (String symbol : splitWords) {
            tokens.add(vocab.getOrDefault(symbol, vocab.get("<UNK>")));
        }

        //getLogger().info("BPE.tokenize.tokens: " + tokens);
        return tokens;
    }
}
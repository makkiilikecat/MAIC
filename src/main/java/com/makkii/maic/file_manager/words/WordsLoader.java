package com.makkii.maic.file_manager.words;

import com.makkii.maic.file_manager.FileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.makkii.maic.AIParams.*;

public class WordsLoader {

    private static final int maxWords = 10000000;

    //public static Set<String> loadWords() throws IOException {
    //    try (Stream<String> lines = Files.lines(FileManager.aiWordsFile.toPath())) {
    //        return lines
    //                .flatMap(line -> Stream.of(line.split(","))) // カンマで分割
    //                //.map(String::trim) // 前後の空白を除去
    //                //.filter(word -> !word.isEmpty()) // 空文字列を除去
    //                .collect(Collectors.toCollection(HashSet::new)); // HashSetに収集
    //    }
    //}

    public static void loadWords() throws IOException {
        // 同期化されたLinkedHashMapを作成
        AtomicInteger index = new AtomicInteger(0); //インデックスをAtomicIntegerにする
        List<String> lines = Files.readAllLines(FileManager.ai_WordsFile.toPath());

        lines.stream()
                .flatMap(line -> java.util.Arrays.stream(line.split(","))) // カンマで分割
                .map(String::trim)       // 空白トリム
                .filter(word -> !word.isEmpty()) // 空文字列は除外
                .forEach(word -> {
                    int currentIndex = index.getAndIncrement();
                    if (currentIndex < maxWords) {  // 仮で語彙数を5000に制限
                        indexToWord.put(currentIndex, word);
                        wordToIndex.put(word, currentIndex);
                    } else {
                        return;
                    }
                });
        wordSize = Math.min(indexToWord.size(), maxWords);
    }

    public static String getWord(int index) {
        return indexToWord.get(index);
    }

    public static Integer getIndex(String word) {
        return wordToIndex.getOrDefault(word, -1);
    }

    /**
     * List< String> を List< Integer> に変換するメソッド。
     * トークン化した文字列をwordIdにするときとか
     *
     * @param words String のリスト
     * @return Integer のリスト
     */
    public static List<Integer> convertToIndexList(List<String> words) {
        return words.stream()
                .map(WordsLoader::getIndex) // 各 String に対して getIndex メソッドを呼び出す
                .collect(Collectors.toList()); // 結果を List<Integer> にまとめる
    }

    /**
     * List< Integer> を List< String> に変換するメソッド。
     * 主にデバッグ用
     *
     * @param ids Integer のリスト
     * @return String のリスト
     */
    public static List<String> convertToWordList(List<Integer> ids) {
        return ids.stream()
                .map(WordsLoader::getWord) // 各 String に対して getIndex メソッドを呼び出す
                .collect(Collectors.toList()); // 結果を List<Integer> にまとめる
    }
}

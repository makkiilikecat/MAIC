package com.makkii.maic.file_manager.words;

import com.makkii.maic.AIParams;
import com.makkii.maic.file_manager.FileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.makkii.maic.AIParams.*;

public enum WordsLoader {
    ;

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
        List<String> lines = Files.readAllLines(FileManager.aiWordsFile.toPath());

        lines.stream()
                .flatMap(line -> java.util.Arrays.stream(line.split(","))) // カンマで分割
                .map(String::trim)       // 空白トリム
                .filter(word -> !word.isEmpty()) // 空文字列は除外
                .forEach(word -> {
                    int currentIndex = index.getAndIncrement();
                    indexToWord.put(currentIndex, word);
                    wordToIndex.put(word, currentIndex);
                });
        wordSize = indexToWord.size();
    }

    public static String getWord(int index) {
        return indexToWord.get(index);
    }

    public static Integer getIndex(String word) {
        return wordToIndex.get(word);
    }

    //public static void main(String[] args) throws IOException {
    //    // 使用例
    //    //System.out.println(wordIndex.getWord(1));       // 出力: Ä
    //    //System.out.println(wordIndex.getIndex("三塁打")); // 出力: 5
    //    //System.out.println(wordIndex.getWord(5));

    //    // マルチスレッドでの読み込み例 (デモンストレーション)
    //    Runnable task = () -> {
    //        for (int i = 0; i < 6; i++) { //全要素にアクセス
    //            String word = wordIndex.getWord(i);
    //            if (word != null) {
    //                System.out.println(Thread.currentThread().getName() + ": Index " + i + " = " + word);
    //            }
    //        }
    //    };

    //    Thread thread1 = new Thread(task);
    //    Thread thread2 = new Thread(task);

    //    thread1.start();
    //    thread2.start();
    //}
}

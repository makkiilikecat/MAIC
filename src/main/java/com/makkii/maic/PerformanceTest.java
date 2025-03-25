package com.makkii.maic;

import static org.bukkit.Bukkit.getLogger;

public enum PerformanceTest {
    ;

    //public static void longStringTable(String string, int length) {
    public static void longStringTable() {

        double random = Math.random();
        double doubleRandom = random;

        long startTime = System.nanoTime();
        // 全体コメントアウトでメモリ4.7GB

        // 通常の文字列連結は10万回で2.19秒、メモリ40GB以上使用（不明）
        //String testString = "";
        //for (int i=0; i<100000; i++) {
        //     testString+= "testString";
        //}

        // StringBuilderだとtoStringしてもしなくても2ミリ秒、メモリ4.8GB使用
        //StringBuilder testString = new StringBuilder();
        //for (int i=0; i<100000; i++) {
        //    testString.append("testString");
        //}
        //String string = testString.toString();

        // String[10万]だと1ミリ秒、メモリ4.7GB使用　最速
        //String testString = "testString";
        //String[] testStringList = new String[100000];
        //for (int i = 0; i<100000; i++){
        //    testStringList[i] = testString;
        //}


        /*
         * double型とdouble型で、キャストなし掛け算の計算速度テスト
         * 結果：double型のほうが倍近く速い、全部doubleからdoubleに置き換えた
         */
        // 16777216(1600万)で42ミリ秒
        //double doubleTest = 50;
        //for (int i = 16777200; i < 1000000000; i++) {
        //    for (double j = 0; j < 16777216; j++) {  // 何故かこの値を超えるとランダムがなんであろうと計算ができない
        //        doubleTest = doubleRandom * j;
        //    }
        //    getLogger().info(String.valueOf(i));
        //}

        //double doubleTest = 50;
        //for (long i = 1000000000; i < 2000000000; i=i+500000000) {
        //for (double j = 0; j < Integer.MAX_VALUE; j++) {    // Integer.MAX_VALUEだと5765ミリ秒
        //for (double j = 0; j < 16777200; j++) {              // 16777200(1600万)だと24ミリ秒
        //    doubleTest = random * j;
        //}
        //    getLogger().info(String.valueOf(i));
        //}

        long endTime = System.nanoTime();

        //if (string.length() == 100) getLogger().info("");     // ダミー　コンパイラがtoStringをなくさないようにする、念の為
        //if (doubleTest == 0) getLogger().info("");
        //if (doubleTest == 0) getLogger().info("");

        //getLogger().info("--------------length: " + testStringList.length);
        getLogger().info("Time: " + (endTime - startTime) / 1000 / 1000);
    }
}

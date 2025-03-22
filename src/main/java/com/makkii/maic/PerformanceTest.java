package com.makkii.maic;

public enum PerformanceTest {
    ;

    public static void longStringTable(String string, int length) {

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

        long endTime = System.nanoTime();
        //if (string.length() == 100) getLogger().info("");     // ダミー　コンパイラがtoStringをなくさないようにする、念の為
        //getLogger().info("--------------length: " + testStringList.length);
        //getLogger().info("Time: " +(endTime-startTime)/1000/1000);
    }
}

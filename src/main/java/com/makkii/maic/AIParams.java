package com.makkii.maic;

import java.util.*;

public enum AIParams {
    ;
    // 定数
    public static final int DIMENSION = 32;//512            // 埋め込みベクトルの次元数
    //public static final int K_Q_DIMENTION = 32;
    public static final int MAX_CONTEXT = 64;//256          // 最大コンテキスト長
    public static final double LEARNING_RATE = 0.001;   // 学習率
    public static final int BATCH_SIZE = 32;            // バッチサイズ
    public static final int EPOCHS = 10;                // エポック数
    public static final int LAYERS = 1;//6                 // レイヤー数

    // サイズ
    public static int wordSize;
    // 語彙リスト　キーからでも値からでも参照が早くなるように
    public static final Map<Integer, String> indexToWord = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final Map<String, Integer> wordToIndex = Collections.synchronizedMap(new LinkedHashMap<>());

    // 埋め込み行列 (ai_vectors に相当)
    public static float[][] embeddingMatrix;
    public static float[][] unEmbeddingMatrix;

    // attention 用の重み行列
    public static float[][][] attentionWeightsQ; // [layer][contextKey][DIMENSION]
    public static float[][][] attentionWeightsK; // [layer][contextKey][DIMENSION]
    public static float[][][] attentionWeightsV; // [layer][contextKey][DIMENSION]

    //playerPerceptron 用の重み行列とバイアス
    public static float[][][] mppWeights1; // [layer][DIMENSION][DIMENSION * 4]
    public static float[][] mppBiases1;  // [layer][DIMENSION * 4]
    public static float[][][] mppWeights2; // [layer][DIMENSION * 4][DIMENSION]
    public static float[][] mppBiases2;  // [layer][DIMENSION]
}
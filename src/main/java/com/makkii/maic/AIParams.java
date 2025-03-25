package com.makkii.maic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AIParams {
    // 定数
    public static final int DIMENSION = 256;//512            // 埋め込みベクトルの次元数
    //public static final int K_Q_DIMENTION = 32;
    public static final int MAX_CONTEXT = 128;//256          // 最大コンテキスト長
    public static final double LEARNING_RATE = 0.01;//0.001   // 学習率
    public static final int BATCH_SIZE = 32;//32            // バッチサイズ
    public static final int EPOCHS = 1;//10                // エポック数
    public static final int LAYERS = 1;//6                 // レイヤー数
    public static final int MAX_WORD_SIZE = 10000;

    // サイズ
    public static int wordSize;
    // 語彙リスト　キーからでも値からでも参照が早くなるように
    public static final Map<Integer, String> indexToWord = Collections.synchronizedMap(new LinkedHashMap<>());
    public static final Map<String, Integer> wordToIndex = Collections.synchronizedMap(new LinkedHashMap<>());

    // 埋め込み行列 (ai_vectors に相当)
    public static double[][] embeddingMatrix;
    public static double[][] unEmbeddingMatrix;

    // attention 用の重み行列
    public static double[][][] attentionWeightsQ; // [layer][contextKey][DIMENSION]
    public static double[][][] attentionWeightsK; // [layer][contextKey][DIMENSION]
    public static double[][][] attentionWeightsV; // [layer][contextKey][DIMENSION]

    //playerPerceptron 用の重み行列とバイアス
    public static double[][][] mppWeights1; // [layer][DIMENSION][DIMENSION * 4]
    public static double[][] mppBiases1;  // [layer][DIMENSION * 4]
    public static double[][][] mppWeights2; // [layer][DIMENSION * 4][DIMENSION]
    public static double[][] mppBiases2;  // [layer][DIMENSION]

    // 各パラメータの勾配を保存するための変数（AIParamsに追加）
    public static double[][][] attentionWeightsQGradients;
    public static double[][][] attentionWeightsKGradients;
    public static double[][][] attentionWeightsVGradients;
    public static double[][][] mppWeights1Gradients;
    public static double[][] mppBiases1Gradients;
    public static double[][][] mppWeights2Gradients;
    public static double[][] mppBiases2Gradients;
    public static double[][] embeddingMatrixGradients;
    public static double[][] unEmbeddingMatrixGradients;
}
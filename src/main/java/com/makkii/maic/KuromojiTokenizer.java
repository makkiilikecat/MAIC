package com.makkii.maic;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public enum KuromojiTokenizer {
    ;

    private static final Tokenizer tokenizer = new Tokenizer();


    public static ArrayList<String> tokenize(String text) {

        List<Token> tokens = tokenizer.tokenize(text);

        // LLMに適した形にするための処理 (単語またはそれ以上の区切り)
        ArrayList<String> resultList = new ArrayList<>();
        StringBuilder currentPhrase = new StringBuilder();

        for (Token token : tokens) {
            String surface = token.getSurface();
            String partOfSpeech = token.getPartOfSpeechLevel1(); // 品詞を取得 (第一レベル)

            // 名詞、動詞、形容詞、副詞などの主要な品詞の場合は、単語として区切る
            if ("名詞".equals(partOfSpeech) || "動詞".equals(partOfSpeech) ||
                    "形容詞".equals(partOfSpeech) || "副詞".equals(partOfSpeech) || "接頭辞".equals(partOfSpeech) || "接続詞".equals(partOfSpeech) || "感動詞".equals(partOfSpeech))
            {
                // 前の句があれば、結果リストに追加
                if (currentPhrase.length() > 0) {
                    resultList.add(currentPhrase.toString());
                    currentPhrase.setLength(0); // Reset
                }
                resultList.add(surface); // 単語を直接追加
            }
            // 助詞、助動詞、記号などの場合は、前の単語と結合する（句を作る）
            else if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech) || "記号".equals(partOfSpeech)) {
                currentPhrase.append(surface);
            }
            // その他の品詞(未知語など)の場合の考慮.  単語として区切る
            else {
                if (currentPhrase.length() > 0) {
                    resultList.add(currentPhrase.toString());
                    currentPhrase.setLength(0);
                }
                resultList.add(surface);
            }

        }
        // 最後の句を処理
        if (currentPhrase.length() > 0) {
            resultList.add(currentPhrase.toString());
        }

        // List<String> から String[] へ変換
        return resultList;
    }

    //public static void main(String[] args) {
    //    String[] tokens = tokenize(args); // コマンドライン引数を使用

    //    // 結果の表示 (確認用)
    //    if (tokens.length > 0 ) {
    //        System.out.println("Tokenized output:");
    //        for (String token : tokens) {
    //            System.out.println(token);
    //        }
    //    }
    //}
}
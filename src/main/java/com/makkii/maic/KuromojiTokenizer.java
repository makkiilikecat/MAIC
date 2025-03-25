package com.makkii.maic;

import com.atilika.kuromoji.TokenBase;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.makkii.maic.file_manager.words.WordsLoader;

import java.util.ArrayList;
import java.util.List;

public class KuromojiTokenizer {

    private static final Tokenizer tokenizer = new Tokenizer();

    private static final ArrayList<String> resultList = new ArrayList<>();

    public static ArrayList<String> tokenize(String text) {

        List<Token> tokens = tokenizer.tokenize(text);
        resultList.clear();
        // 名詞、動詞、形容詞、副詞などの主要な品詞の場合は、単語として区切る
        //if ("名詞".equals(partOfSpeech) || "動詞".equals(partOfSpeech) ||
        //        "形容詞".equals(partOfSpeech) || "副詞".equals(partOfSpeech) || "接頭辞".equals(partOfSpeech) ||
        //        "接続詞".equals(partOfSpeech) || "感動詞".equals(partOfSpeech) || "記号".equals(partOfSpeech) ||
        //        "助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech))
        //{
        // 前の句があれば、結果リストに追加
        //}
        // 助詞、助動詞、記号などの場合は、前の単語と結合する（句を作る）
        //else if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
        //    currentPhrase.append(surface);
        //}
        // その他の品詞(未知語など)の場合の考慮.  単語として区切る
        //else {
        //    if (currentPhrase.length() > 0) {
        //        resultList.add(currentPhrase.toString());
        //        currentPhrase.setLength(0);
        //    }
        //    resultList.add(surface);
        //}
        tokens.stream().map(TokenBase::getSurface).forEach(resultList::add);

        // List<String> から String[] へ変換
        return resultList;
    }


    /**
     * テキストをトークン化し、語彙リストに基づいて未知語処理を行います。
     *
     * @param text トークン化するテキスト
     * @return トークンのリスト
     */
    public static ArrayList<String> promptTokenize(String text) {
        List<Token> tokens = tokenizer.tokenize(text);
        ArrayList<String> resultList = new ArrayList<>();

        for (Token token : tokens) {
            String surface = token.getSurface();

            if (WordsLoader.getIndex(surface) != -1) {
                resultList.add(surface); // 既知の語彙
            } else {
                processUnknownWord(surface, resultList); // 未知語の処理
            }
        }
        return resultList;
    }

    /**
     * 未知語を処理し、語彙リストに存在する部分文字列に分割しようと試みます。
     *
     * @param unknownWord 未知語
     * @param resultList  結果リスト
     */
    private static void processUnknownWord(String unknownWord, ArrayList<String> resultList) {
        int len = unknownWord.length();
        for (int i = len; i > 0; i--) {
            String prefix = unknownWord.substring(0, i);

            if (WordsLoader.getIndex(prefix) != -1) {
                resultList.add(prefix);
                if (i < len) {
                    processUnknownWord(unknownWord.substring(i), resultList); // 残りの部分を再帰的に処理
                }
                return;
            }
        }

        // どの部分文字列も語彙リストに見つからなかった場合、1文字ずつ削って再試行
        int startIndex = 0;
        while (startIndex < len) {
            boolean found = false;
            for (int endIndex = len; endIndex > startIndex; endIndex--) {
                String sub = unknownWord.substring(startIndex, endIndex);
                if (WordsLoader.getIndex(sub) != -1) {
                    resultList.add(sub);
                    startIndex = endIndex;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // 1文字を未知語として追加（これ以上分割できない）
                resultList.add(unknownWord.substring(startIndex, startIndex + 1));
                startIndex++;
            }
        }
    }

    /**
     * シンプルなトークナイザー。語彙リスト構築時などに使用。
     *
     * @param text 分割したいテキスト
     * @return 分割された String の ArrayList
     */
    public static ArrayList<String> simpleTokenize(String text) {
        List<Token> tokens = tokenizer.tokenize(text);
        ArrayList<String> resultList = new ArrayList<>();
        for (Token token : tokens) {
            resultList.add(token.getSurface());
        }
        return resultList;
    }
}
package com.makkii.maic.file_manager;

import com.atilika.kuromoji.ipadic.Tokenizer;
import com.makkii.maic.AIParams;
import com.makkii.maic.KuromojiTokenizer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.makkii.maic.AIParams.MAX_CONTEXT;
import static org.bukkit.Bukkit.getLogger;

public class SentenceLabeler {

    // 文末に使う文字列
    //private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
    //        //"（）]「」【】『』\"'`<>〈〉《》〔〕〚〛〘〙〝〟«»‹›。!?|！？｜"
    //        "[。！？!?]"
    //);
    // 連続する空白文字（改行、スペース、タブなど）にマッチする正規表現
    //private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    //private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[, 　\\t\\n\\r]+");

    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。！？!?]");
    public static final Pattern WHITESPACE_PATTERN = Pattern.compile("[ 　\\t\\n\\r]+");
    // 文の途中での分割に使用するパターン（読点、句読点、空白文字など）
    private static final Pattern SENTENCE_MIDDLE_PATTERN = Pattern.compile("[、，,.\\s]");

    //private static final int MAX_CONTEXT = 10; // 最大トークン数（例として小さめの値に設定）
    private static final Tokenizer tokenizer = new Tokenizer();


//    public static List<String> splitIntoSentences() {
//
//        //getLogger().info("[MAIC] databaseNodeList: " + FileManager.dataBaseNodeList);
//
//        List<String> sentences = new ArrayList<>();
//        for (int temp = 0; temp < FileManager.databaseNodeLength; temp++) {
//
//            Node nNode = FileManager.dataBaseNodeList.item(temp);
//            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//                Element eElement = (Element) nNode;
//                String title = eElement.getAttribute("title");
//
//                // StringBuilder を使用して combinedText を構築
//                StringBuilder combinedText = new StringBuilder(title.length() + 1024); // content のおおよその長さを加える
//                //combinedText.append(title);
//                //combinedText.append('\n');
//
//                // content 内の不要な空白と改行を削除しながら combinedText に追加
//                NodeList children = eElement.getChildNodes();
//                for (int i = 0; i < children.getLength(); i++) {
//                    Node child = children.item(i);
//                    if (child.getNodeType() == Node.TEXT_NODE) {
//                        String textContent = child.getTextContent();
//                        // 連続する空白と改行を単一の改行に置き換える
//                        //boolean lastWasNewline = false;
//                        //for (char c : textContent.toCharArray()) {
//                        //    if (c == '\n') {
//                        //        if (!lastWasNewline) {
//                        //            //combinedText.append('\n');
//                        //            lastWasNewline = true;
//                        //        }
//                        //    } else if (!Character.isWhitespace(c)) { //空白文字以外
//                        //        combinedText.append(c);
//                        //        lastWasNewline = false;
//                        //    }
//                        //}
//
//                        // 連続する空白文字を1つのスペースに置換
//                        //String normalizedText = WHITESPACE_PATTERN.matcher(textContent).replaceAll(" ");
//                        // 空白文字を全て削除
//                        String normalizedText = WHITESPACE_PATTERN.matcher(textContent).replaceAll(" ");
//
//                        //List<String> sentenceList = new ArrayList<>();
//                        Matcher matcher = SENTENCE_END_PATTERN.matcher(normalizedText);
//                        int start = 0;
//                        while (matcher.find()) {
//                            //sentenceList.add(normalizedText.substring(start, matcher.end()));
//                            combinedText.append(normalizedText, start, matcher.end());
//                            start = matcher.end();
//                        }
//                        if (start < normalizedText.length()) {
//                            //sentenceList.add(normalizedText.substring(start));
//                            combinedText.append(normalizedText.substring(start));
//                        }
//                    }
//                }
//
//                // HashSet から不要な要素を削除
//                Set<String> unwantedChars = new HashSet<>(
//                        Arrays.asList(",", " ", "　", "\n", "\r", "\t", "\\n", "\\r", "\\t")
//                ); // 全角空白、CR, LF, タブ
//                String sortedWords = combinedText.toString().replaceAll(String.join("", unwantedChars), "");
//
//                // 文分割処理
//                Matcher matcher = SENTENCE_END_PATTERN.matcher(sortedWords);
//                int start = 0;
//                while (matcher.find()) {
//                    String sentence = sortedWords.substring(start, matcher.end()).trim();
//                    if (!sentence.isEmpty()) {
//                        sentences.add(sentence);
//                    }
//                    start = matcher.end();
//                }
//
//                // 残りの部分を処理
//                String remaining = sortedWords.substring(start).trim();
//                if (!remaining.isEmpty()) {
//                    sentences.add(remaining);
//                }
//            }
//
//            // テスト用に、文を1つに制限
//            break;
//        }
//        //getLogger().info("[MAIC] 文分割が完了しました。");
//        getLogger().info("文章の数"+sentences.size());
//        getLogger().info("文章0: " + sentences.get(0));
//        return sentences;
//    }

    public static List<String> basedataSplit() {
        //getLogger().info("[MAIC] databaseNodeList: " + FileManager.dataBaseNodeList);

        List<String> sentences = new ArrayList<>();
        for (int temp = 0; temp < FileManager.databaseNodeLength; temp++) {

            Node nNode = FileManager.dataBaseNodeList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                //String title = eElement.getAttribute("title");

                // content 内の不要な空白と改行を削除しながら combinedText に追加
                NodeList children = eElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        String textContent = child.getTextContent();
                        sentences.addAll(splitAndTokenize(textContent));

                        if (sentences.size() >= AIParams.BATCH_SIZE) {  // 文の数を10に制限
                            getLogger().info("文章の数" + sentences.size());
                            //getLogger().info("文章0: " + sentences.get(0));
                            return sentences;
                        }
                    }
                }
            }
        }

        //getLogger().info("[MAIC] 文分割が完了しました。");
        getLogger().info("文章の数" + sentences.size());
        //getLogger().info("文章0: " + sentences.get(0));
        return sentences;
    }

    private static final List<String> sentenceList = new ArrayList<>();

    public static List<String> splitIntoSentences(String text) {
        // 空白文字を削除
        String cleanedText = WHITESPACE_PATTERN.matcher(text).replaceAll("");

        sentenceList.clear();
        Matcher matcher = SENTENCE_END_PATTERN.matcher(cleanedText);
        int start = 0;
        while (matcher.find()) {
            sentenceList.add(cleanedText.substring(start, matcher.end()));
            start = matcher.end();
        }
        if (start < cleanedText.length()) {
            sentenceList.add(cleanedText.substring(start));
        }
        return sentenceList;
    }


    private static final List<String> result = new ArrayList<>();

    public static List<String> splitAndTokenize(String inputText) {
        List<String> sentences = splitIntoSentences(inputText);     // 文で区切る
        result.clear();

        for (String sentence : sentences) {
            List<String> tokens = KuromojiTokenizer.tokenize(sentence);

            // 文のトークンサイズがMAX_CONTEXTを超えていたらら、より多く分割する
            if (tokens.size() <= MAX_CONTEXT) {
                result.add(String.join("", tokens));
            } else {
                // 文の途中での分割を試みる
                List<String> subSentences = splitSentenceByMiddlePattern(sentence);

                for (String subSentence : subSentences) {
                    List<String> subTokens = KuromojiTokenizer.tokenize(subSentence);

                    // それでもMAX_CONTEXTを超えていたら無理やり分割して、MAX_CONTEXTを超えないようにする
                    if (subTokens.size() <= MAX_CONTEXT) {
                        result.add(String.join("", subTokens));
                    } else {
                        // 無理やり分割
                        for (int i = 0; i < subTokens.size(); i += MAX_CONTEXT) {
                            int end = Math.min(i + MAX_CONTEXT, subTokens.size());
                            result.add(String.join("", subTokens.subList(i, end)));
                        }
                    }
                }
            }
        }
        return result;
    }


    private static final List<String> filteredSubSentences = new ArrayList<>();

    private static List<String> splitSentenceByMiddlePattern(String sentence) {
        List<String> subSentences = new ArrayList<>();
        Matcher matcher = SENTENCE_MIDDLE_PATTERN.matcher(sentence);
        int start = 0;
        while (matcher.find()) {
            subSentences.add(sentence.substring(start, matcher.end()));
            start = matcher.end();
        }
        if (start < sentence.length()) {
            subSentences.add(sentence.substring(start));
        }

        // 空の要素や空白だけの要素を取り除く
        filteredSubSentences.clear();
        for (String sub : subSentences) {
            if (!sub.trim().isEmpty()) {
                filteredSubSentences.add(sub);
            }
        }
        return filteredSubSentences;
    }
}

package com.makkii.maic.file_manager;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getLogger;

public class SentenceLabeler {

    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("([。？！])([^」』】）])*");

    public static List<String> splitIntoSentences() {

        //getLogger().info("[MAIC] databaseNodeList: " + FileManager.dataBaseNodeList);

        List<String> sentences = new ArrayList<>();
        for (int temp = 0; temp < FileManager.databaseNodeLength; temp++) {

            Node nNode = FileManager.dataBaseNodeList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String title = eElement.getAttribute("title");

                // StringBuilder を使用して combinedText を構築
                StringBuilder combinedText = new StringBuilder(title.length() + 1024); // content のおおよその長さを加える
                combinedText.append(title);
                //combinedText.append('\n');

                // content 内の不要な空白と改行を削除しながら combinedText に追加
                NodeList children = eElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        String textContent = child.getTextContent();
                        // 連続する空白と改行を単一の改行に置き換える
                        boolean lastWasNewline = false;
                        for (char c : textContent.toCharArray()) {
                            if (c == '\n') {
                                if (!lastWasNewline) {
                                    //combinedText.append('\n');
                                    lastWasNewline = true;
                                }
                            } else if (!Character.isWhitespace(c)) { //空白文字以外
                                combinedText.append(c);
                                lastWasNewline = false;
                            }
                        }
                    }
                }

                // HashSet から不要な要素を削除
                //String sortedWords = combinedText.toString().replaceAll(" 　\n\r\t\\n\\r\\t", "");

                // 文分割処理
                Matcher matcher = SENTENCE_END_PATTERN.matcher(combinedText);
                int start = 0;
                while (matcher.find()) {
                    String sentence = combinedText.substring(start, matcher.end()).trim();
                    if (!sentence.isEmpty()) {
                        sentences.add(sentence);
                    }
                    start = matcher.end();
                }

                // 残りの部分を処理
                String remaining = combinedText.substring(start).trim();
                if (!remaining.isEmpty()) {
                    sentences.add(remaining);
                }
            }
        }
        //getLogger().info("[MAIC] 文分割が完了しました。");
        return sentences;
    }
}

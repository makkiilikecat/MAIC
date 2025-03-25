package com.makkii.maic.file_manager.words;

import com.makkii.maic.KuromojiTokenizer;
import com.makkii.maic.file_manager.FileManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static com.makkii.maic.AIParams.MAX_WORD_SIZE;
import static com.makkii.maic.file_manager.SentenceLabeler.WHITESPACE_PATTERN;
import static org.bukkit.Bukkit.getLogger;

public class WordsCreater {


    /*
     * ai_basedata.xmlをベースにai_words.txtを作成する。
     * R5 7500F、他のタスクつけっぱなしだと20秒かかる（約20万語彙）
     * まだトークン化が適切に出来ていないため、語彙数が過度に増えてしまう
     */
    public static void main() {

        /*try {  // 1. XMLファイルを文字列として読み込む
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    Files.newInputStream(FileManager.aiDatabaseFile.toPath()), StandardCharsets.UTF_8)))
            {
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    contentBuilder.append(sCurrentLine).append("\n");
                }
            }

            // 2. 擬似的なルート要素で囲む
            String xmlContent = "<root>\n" + contentBuilder + "\n</root>";

            // 3. 文字列からDocumentオブジェクトを作成
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            // 2. <doc>要素のリストを取得
            NodeList nList = doc.getElementsByTagName("doc");
            getLogger().info("[MAIC] トークン化中..." + nList.getLength() + "件");

            // 3. トークン化された単語を格納するためのHashSet
            Set<String> uniqueWords = new HashSet<>();

            // 4. 各<doc>要素を処理
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    // タイトルと本文の内容を取得
                    Node titleNode = eElement.getElementsByTagName("title").item(0);
                    String title = ""; // or some default value
                    if (titleNode != null) {
                        title = titleNode.getTextContent();
                    } else {
                        System.err.println("Warning: <doc> element with id " + eElement.getAttribute("id") + " has no <title> element.");
                        // または、ここで処理を中断する (continue; など)
                    }
                    String content = eElement.getTextContent();
                    String combinedText = title + "\n" + content;
                    combinedText = combinedText.replaceAll("\\n+", "\n");
                    String[] tokens = KuromojiTokenizer.tokenize(combinedText);

                    // トークンをHashSetに追加（重複は自動的に排除）
                    //Arrays.stream(tokens).forEach(uniqueWords::add); //ストリームを使う場合
                    uniqueWords.addAll(Arrays.asList(tokens));
                }
            }

            // 5. HashSetの内容をファイルに書き出す
            try (PrintWriter writer = new PrintWriter(FileManager.aiWordsFile, "UTF-8")) {
                String result = String.join(",", uniqueWords);
                writer.println(result);
            } catch (IOException e) {
                System.err.println("Error writing to ai_words.txt: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }*/
        try {
            int wordsCount = 0;
            Set<String> uniqueWords = new HashSet<>();
            /*
            for (int temp = 0; temp < nListLength; temp++) {

                if (temp % 50 == 0) getLogger().info(temp + "/" + nListLength);

                nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    eElement = (Element) nNode;

                    // title属性の値を取得する
                    content.append(eElement.getTextContent());

                    StringBuilder combinedText = new StringBuilder(title.length() + content.length() + 1); // 初期容量を見積もる
                    combinedText.append(title).append('\n').append(content);

                    //combinedText = PATTERN.matcher(combinedText).replaceAll("\n");
                    // replace を繰り返し使う (replaceAll よりは高速)
                    String combinedTextString = combinedText.toString();
                    while (combinedTextString.contains("\n\n")) {
                        combinedTextString = combinedTextString.replace("\n\n", "\n");
                    }

                    // トークン化
                    Collection<String> tokens = KuromojiTokenizer.tokenize(combinedText.toString());

                    uniqueWords.addAll(tokens);
                }
            }
            */
            getLogger().info("[MAIC] " + FileManager.databaseNodeLength + "件のページをトークン化中...");

            for (int temp = 0; temp < FileManager.databaseNodeLength; temp++) {
                if (temp % 1000 == 0) {
                    getLogger().info("[MAIC] 進捗: " +
                            (float) (temp / FileManager.databaseNodeLength * 100) + "% " +
                            temp + "/" + FileManager.databaseNodeLength);
                }

                Node nNode = FileManager.dataBaseNodeList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String title = eElement.getAttribute("title");

                    // StringBuilder を使用して combinedText を構築
                    StringBuilder combinedText = new StringBuilder(title.length() + 100000); // content のおおよその長さを加える
                    combinedText.append(title);

                    // content 内の不要な空白と改行を削除しながら combinedText に追加
                    NodeList children = eElement.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node child = children.item(i);
                        if (child.getNodeType() == Node.TEXT_NODE) {
                            String textContent = child.getTextContent();

                            // 空白や改行を1つの半角空白に置き換える
                            //combinedText = combinedText.append(WHITESPACE_PATTERN.matcher(textContent).replaceAll(" "));
                            // 空白や改行を無くす
                            String normalizedText = WHITESPACE_PATTERN.matcher(textContent).replaceAll("");
                            if (!normalizedText.isEmpty()) {
                                combinedText.append(normalizedText);
                            }
                        }
                    }

                    // トークンの最大文字数制限
                    int maxChunkSize = 20;
                    String chunkSplitText = combinedText.toString();
                    for (int i = 0; i < chunkSplitText.length(); i += maxChunkSize) {
                        List<String> tokens = KuromojiTokenizer.tokenize(
                                chunkSplitText.substring(i, Math.min(chunkSplitText.length(), i + maxChunkSize))
                        );

                        // トークンで使われている文字全てを1文字で区切って追加（語彙サイズが小さい場合に未知語を減らす）
                        for (String str : tokens) {
                            for (char c : str.toCharArray()) {
                                uniqueWords.add(String.valueOf(c));
                            }
                        }
                        uniqueWords.addAll(tokens);
                    }
                }
            }

            getLogger().info("[MAIC] トークン化が終了しました。" + FileManager.AI_WORDS_FILE_NAME + "として保存しています...");

            try (PrintWriter writer = new PrintWriter(FileManager.ai_WordsFile, "UTF-8")) {
                // 不要な要素のセットを作成
                Set<String> unwantedChars = new HashSet<>(
                        Arrays.asList(",", " ", "　", "\n", "\r", "\t", "\\n", "\\r", "\\t", " ")
                        //Arrays.asList(",")
                ); // 全角空白、CR, LF, タブ

                // 不要な文字を削除、文字数が0の要素も削除
                uniqueWords = uniqueWords.stream()
                        .map(word -> word.chars()
                                .filter(c -> !unwantedChars.contains((char) c))
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString())
                        .filter(s -> !s.isEmpty())  // 空文字列を削除
                        .collect(Collectors.toSet());

                // 文字数順にソート
                List<String> sortedWords = uniqueWords.parallelStream()
                        .sorted(Comparator.comparingInt(String::length))
                        .collect(Collectors.toList());

                // 短い文字列順に、語彙数を制限（長い固有名詞を含みにくくするため）
                String output = String.join(",", sortedWords.subList(0, Math.min(sortedWords.size(), MAX_WORD_SIZE)));
                writer.print(output);

            } catch (FileNotFoundException e) {
                System.err.println("Error writing to ai_words.txt: " + e.getMessage());
            }

            getLogger().info("[MAIC] " + FileManager.AI_WORDS_FILE_NAME + "の保存が完了しました。");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

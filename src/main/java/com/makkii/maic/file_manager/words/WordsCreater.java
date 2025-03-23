package com.makkii.maic.file_manager.words;

import com.makkii.maic.KuromojiTokenizer;
import com.makkii.maic.file_manager.FileManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getLogger;

public enum WordsCreater {
    ;

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
            // 1. XMLファイルを文字列として読み込む
            //StringBuilder contentBuilder = new StringBuilder();
            //try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(FileManager.aiDatabaseFile.toPath()), StandardCharsets.UTF_8))) {
            //    String sCurrentLine;
            //    while ((sCurrentLine = br.readLine()) != null) {
            //        contentBuilder.append(sCurrentLine).append("\n");
            //    }
            //}

            // 2. 擬似的なルート要素で囲む
            //String xmlContent = "<root>\n" + contentBuilder + "\n</root>";
            //getLogger().info("[MAIC] " + xmlContent);

            // 3. 文字列からDocumentオブジェクトを作成
            //DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            //DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            //Document doc = dBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            //doc.getDocumentElement().normalize();

            // 以降は元のコードと同じ（<doc>要素の処理）
            //NodeList nList = doc.getElementsByTagName("doc");

            //getLogger().info("[MAIC] " + FileManager.dataBaseNodeList.item(5).getTextContent());

            Set<String> uniqueWords = new HashSet<>();

            // 毎回インスタンス生成するとメモリ消費が増えるためここで生成
            //StringBuilder title = new StringBuilder();
            //StringBuilder content = new StringBuilder();
            //String combinedText;
            //Node nNode;
            //Element eElement;
            //int nListLength = nList.getLength();

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

            int lastParcentage = 1;
            for (int temp = 0; temp < FileManager.databaseNodeLength; temp++) {
                if ((int) (temp / FileManager.databaseNodeLength * 100) != lastParcentage) {
                    lastParcentage = temp / FileManager.databaseNodeLength * 100;
                    getLogger().info("[MAIC] 進捗: " +
                            (int) (temp / FileManager.databaseNodeLength * 100) + "% " +
                            temp + "/" + FileManager.databaseNodeLength);
                }

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
                                        combinedText.append('\n');
                                        lastWasNewline = true;
                                    }
                                } else if (!Character.isWhitespace(c)) { //空白文字以外
                                    combinedText.append(c);
                                    lastWasNewline = false;
                                }
                            }
                        }
                    }

                    int maxChunkSize = 1000000;
                    String chunkSplitText = combinedText.toString();
                    for (int i = 0; i < chunkSplitText.length(); i += maxChunkSize) {
                        Collection<String> tokens = KuromojiTokenizer.tokenize(
                                chunkSplitText.substring(i, Math.min(chunkSplitText.length(), i + maxChunkSize))
                        );
                        uniqueWords.addAll(tokens);
                    }

                    //明示的にGCを呼ぶ（非推奨だが、状況によっては有効な場合がある）
                    combinedText = null; // combinedTextへの参照をなくす
                }
            }

            getLogger().info("[MAIC] トークン化が完了しました。文字数順に並べ替えています...");

            List<String> sortedWords = uniqueWords.parallelStream()
                    .sorted(Comparator.comparingInt(String::length))
                    .collect(Collectors.toList());

            getLogger().info("[MAIC] 並べ替えが終了しました。" + FileManager.AI_WORDS_FILE_NAME + "として保存しています...");

            try (PrintWriter writer = new PrintWriter(FileManager.ai_WordsFile, "UTF-8")) {
                // 不要な要素のセットを作成
                Set<String> unwantedChars = new HashSet<>(
                        Arrays.asList(" ", "　", "\n", "\r", "\t", "\\n", "\\r", "\\t")
                ); // 全角空白、CR, LF, タブ

                // HashSet から不要な要素を削除
                sortedWords.remove(unwantedChars);
                sortedWords.remove(",");    // 区切り文字なので削除

                // String.join() で結合した後、replaceAll() で不要な文字を削除
                String result = String.join(",", sortedWords)
                        //[]で囲むことですべての不要文字に対してor条件でマッチング
                        .replaceAll("[" + String.join("", unwantedChars) + "]", "");
                //.replaceAll(String.join("", unwantedChars), "");

                writer.print(sortedWords); // println() ではなく print() を使用. println()だと最後に改行が入ってしまう。
            } catch (FileNotFoundException e) {
                System.err.println("Error writing to ai_words.txt: " + e.getMessage());
            }

            getLogger().info("[MAIC] " + FileManager.AI_WORDS_FILE_NAME + "の保存が完了しました。");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

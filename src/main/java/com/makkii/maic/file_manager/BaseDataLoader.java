package com.makkii.maic.file_manager;

import com.makkii.maic.AIParams;
import com.makkii.maic.KuromojiTokenizer;
import com.makkii.maic.file_manager.words.WordsLoader;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.makkii.maic.AIParams.MAX_CONTEXT;
import static org.bukkit.Bukkit.getLogger;

public class BaseDataLoader {

    public static void loadBaseData() {

        try {
            // 1. XMLファイルを文字列として読み込む
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(FileManager.aiBaseDataFile.toPath()), StandardCharsets.UTF_8))) {
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
            Document doc = dBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            // 以降は元のコードと同じ（<doc>要素の処理）
            FileManager.dataBaseNodeList = doc.getElementsByTagName("doc");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 文のリストをミニバッチに分割します。
     *
     * @param sentences 文のリスト (SentenceLabeler から取得)
     * @return ミニバッチのリスト (各ミニバッチはトークンインデックスのリスト)
     */
    /*
    public static List<List<Integer>> createMiniBatches(List<String> sentences, int batchSize) {
        List<List<Integer>> miniBatches = new ArrayList<>();

        for (String sentence : sentences) {
            // 1. 文をトークン化
            ArrayList<String> tokens = KuromojiTokenizer.tokenize(sentence);
            //getLogger().info("tokensize: " + tokens);

            // 2. トークンをインデックスに変換
            //List<Integer> tokenIndices = new ArrayList<>();
            //for (String token : tokens) {
            //    Integer index = WordsLoader.getIndex(token);
            //    // 語彙にない単語は無視（または特別なトークン<unk>に置き換え）
            //    if (index != -1) {
            //        if (tokenIndices.size() < MAX_CONTEXT) tokenIndices.add(index);
            //        //getLogger().info(token);
            //    } else {
            //        //getLogger().warning(token);
            //    }
            //}

            List<Integer> tokenIndices = WordsLoader.convertToIndexList(tokens);

            // 未知語が含まれる要素を削除
            tokenIndices = tokenIndices.stream()
                    .filter(index -> index != -1)
                    .collect(Collectors.toList());

            // 3. MAX_CONTEXT を超える場合は切り詰め
            //getLogger().info(String.valueOf(tokenIndices.size()));
            //if (tokenIndices.size() >= AIParams.MAX_CONTEXT) {
            //    tokenIndices = tokenIndices.subList(0, AIParams.MAX_CONTEXT);
            //}
            ////getLogger().info(String.valueOf(tokenIndices.size()));

            // バッチに追加
            if (tokenIndices.size() > 0) { // 空の文は追加しない
                // バッチサイズになるまで、リストに追加し続ける
                if(miniBatches.size() == 0 || miniBatches.get(miniBatches.size() - 1).size() == batchSize){
                    // 新しいバッチを作成
                    miniBatches.add(new ArrayList<>());
                }
                // リストの最後に追加
                miniBatches.get(miniBatches.size() - 1).addAll(tokenIndices);
            }

            //getLogger().info("size: " + tokenIndices.size());
            //getLogger().info(Arrays.toString(tokenIndices.toArray()));
            // 3. MAX_CONTEXT を超える場合は切り詰め
            //if (tokenIndices.size() > MAX_CONTEXT) {
            //    tokenIndices = tokenIndices.subList(0, MAX_CONTEXT);
            //}
            //getLogger().info(Arrays.toString(tokenIndices.toArray()));

            // バッチに追加
            if (tokenIndices.size() > 0) { // 空の文は追加しない
                // バッチサイズになるまで、リストに追加し続ける
                // MAX_CONTEXTを超えないように分割
                //for (int i = 0; i < tokenIndices.size(); i += MAX_CONTEXT) {
                //    List<Integer> subList = tokenIndices.subList(i, Math.min(i + MAX_CONTEXT, tokenIndices.size()));
                //    //getLogger().info("subListLength: " + subList.size());
                //    if (miniBatches.isEmpty() || miniBatches.get(miniBatches.size() - 1).size() == batchSize) {
                //        // 新しいバッチを作成
                //        miniBatches.add(new ArrayList<>());
                //        //getLogger().info("new batch");
                //    }
                //    // リストの最後に追加
                //    miniBatches.get(miniBatches.size() - 1).addAll(subList);
                //}
                //miniBatches.add(tokenIndices);
            }
        }

        // バッチサイズより小さいバッチを削除
        miniBatches.removeIf(batch -> batch.size() < batchSize);

        return miniBatches;
    }
    */
    public static List<List<Integer>> createMiniBatches(List<String> sentences) {
        List<List<Integer>> miniBatches = new ArrayList<>();

        int batchCount = 0;
        for (String sentence : sentences) {
            ArrayList<String> tokens = KuromojiTokenizer.tokenize(sentence);
            List<Integer> tokenIndices = WordsLoader.convertToIndexList(tokens);
            tokenIndices = tokenIndices.stream()
                    .filter(index -> index != -1)
                    .collect(Collectors.toList());


            // 段階的に短い句を生成
            //for (int i = 0; i < tokenIndices.size(); i++) {
            for (int j = 2; j <= tokenIndices.size(); j++) {                  // トークン1つずつ短いバージョンを
                if (j > MAX_CONTEXT) continue;
                batchCount++;
                if (batchCount > AIParams.BATCH_SIZE) return miniBatches;
                miniBatches.add(tokenIndices.subList(0, j));       // 切り出して追加する
                getLogger().info("batchCount: " + batchCount + ", i: " + tokenIndices.size() + ", j: " + j + Arrays.toString(tokenIndices.subList(0, j).toArray()));
            }
            // "the fluffy blue creature roamed the verdant forest" の場合
            // 最初のループ(i=0): "the"
            // 2番目のループ(i=1): "the fluffy"
            // ...
            // 最後から2番目のループ: "the fluffy blue creature roamed the verdant"
            // 最後のループ:           "the fluffy blue creature roamed the verdant forest" (これは最初に追加済みなので、ここでは追加しない)
            //}


            if (!tokenIndices.isEmpty()) {
                //    // ここでスライディングウィンドウを適用
                //    for (int i = 0; i <= tokenIndices.size() - MAX_CONTEXT; i++) {
                //        batchCount++;
                //        if (batchCount > AIParams.BATCH_SIZE) return miniBatches;
                //        List<Integer> subSequence = tokenIndices.subList(i, i + MAX_CONTEXT);
                //        miniBatches.add(subSequence);
                //        getLogger().info("長い文"+Arrays.toString(WordsLoader.convertToWordList(subSequence).toArray()));
                //    }
                //    // サイズがmaxContext以下となるバッチを追加する。
                //    if(tokenIndices.size() < MAX_CONTEXT) {
                //        batchCount++;
                //        if (batchCount > AIParams.BATCH_SIZE) return miniBatches;
                //        miniBatches.add(tokenIndices);
                //        getLogger().info("短い文" + Arrays.toString(WordsLoader.convertToWordList(tokenIndices).toArray()));
                //    }
            }
        }
        return miniBatches;
    }
}

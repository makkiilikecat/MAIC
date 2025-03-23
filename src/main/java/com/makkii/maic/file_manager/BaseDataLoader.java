package com.makkii.maic.file_manager;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
}

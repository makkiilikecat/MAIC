package com.makkii.maic.file_manager;

import com.makkii.maic.file_manager.words.WordsCreater;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;

import static com.makkii.maic.MainAI.mainAI;
import static org.bukkit.Bukkit.getLogger;

public enum FileManager {
    ;

    public static final String AI_BASEDATA_FILE_NAME = "basedata/ai_basedata.xml";
    public static final String AI_WORDS_FILE_NAME = "basedata/ai_words.txt";
    public static final String AI_Q_VECTORS_FILE_NAME = "attention/ai_Q_vectors.txt";
    public static final String AI_K_VECTORS_FILE_NAME = "attention/ai_K_vectors.txt";
    public static final String AI_V_VECTORS_FILE_NAME = "attention/ai_V_vectors.txt";

    public static File aiBaseDataFile;
    public static File ai_WordsFile;
    public static File ai_Q_VectorsFile;
    public static File ai_K_VectorsFile;
    public static File ai_V_VectorsFile;

    public static boolean requireWordsFile;
    public static boolean require_Q_VectorsFile;
    public static boolean require_K_VectorsFile;
    public static boolean require_V_VectorsFile;

    public static NodeList dataBaseNodeList;
    public static int databaseNodeLength;

    public static void init() throws IOException {

        // ai_basedata.xml
        aiBaseDataFile = new File(mainAI.getDataFolder(), AI_BASEDATA_FILE_NAME);
        if (!aiBaseDataFile.exists()) {
            throw new IOException(AI_BASEDATA_FILE_NAME + "が見つかりません。");
        }

        // 語彙リスト
        ai_WordsFile = new File(mainAI.getDataFolder(), AI_WORDS_FILE_NAME);
        if (!ai_WordsFile.exists()) {
            requireWordsFile = true;   // 作成したファイルは空なので、語彙リストを作成してもらうためのフラグ
            if (!ai_WordsFile.createNewFile()) {  // ファイルがなければ作成
                getLogger().warning("[MAIC] " + AI_WORDS_FILE_NAME + "の作成に失敗しました。");
            }
        }

        // Attention用
        ai_Q_VectorsFile = new File(mainAI.getDataFolder(), AI_Q_VECTORS_FILE_NAME);
        if (!ai_Q_VectorsFile.exists()) {
            require_Q_VectorsFile = true;   // 作成したファイルは空なので、語彙リストを作成してもらうためのフラグ
            if (!ai_Q_VectorsFile.createNewFile()) {  // ファイルがなければ作成
                getLogger().warning("[MAIC] " + AI_Q_VECTORS_FILE_NAME + "の作成に失敗しました。");
            }
        }
        ai_K_VectorsFile = new File(mainAI.getDataFolder(), AI_K_VECTORS_FILE_NAME);
        if (!ai_K_VectorsFile.exists()) {
            require_K_VectorsFile = true;   // 作成したファイルは空なので、語彙リストを作成してもらうためのフラグ
            if (!ai_K_VectorsFile.createNewFile()) {  // ファイルがなければ作成
                getLogger().warning("[MAIC] " + AI_K_VECTORS_FILE_NAME + "の作成に失敗しました。");
            }
        }
        ai_V_VectorsFile = new File(mainAI.getDataFolder(), AI_V_VECTORS_FILE_NAME);
        if (!ai_V_VectorsFile.exists()) {
            require_V_VectorsFile = true;   // 作成したファイルは空なので、語彙リストを作成してもらうためのフラグ
            if (!ai_V_VectorsFile.createNewFile()) {  // ファイルがなければ作成
                getLogger().warning("[MAIC] " + AI_V_VECTORS_FILE_NAME + "の作成に失敗しました。");
            }
        }


        getLogger().info("[MAIC] フォルダ名一覧\n" +
        "     ---------------------------------------------------------\n" +
        "     | PluginDataFolder   Exists: " + mainAI.getDataFolder().exists() + ", " + mainAI.getDataFolder() + "\n" +
        "     | DatabaseFileName   Exists: " + aiBaseDataFile.exists() + ", " + AI_BASEDATA_FILE_NAME + "\n" +
        "     | WordsFileName      Exists: " + ai_WordsFile.exists() + ", " + AI_WORDS_FILE_NAME + "\n" +
        "     | Q_VectorsFileName  Exists: " + ai_Q_VectorsFile.exists() + ", " + AI_Q_VECTORS_FILE_NAME + "\n" +
        "     | K_VectorsFileName  Exists: " + ai_K_VectorsFile.exists() + ", " + AI_K_VECTORS_FILE_NAME + "\n" +
        "     | V_VectorsFileName  Exists: " + ai_V_VectorsFile.exists() + ", " + AI_V_VECTORS_FILE_NAME + "\n" +
        "     ---------------------------------------------------------");


        if (requireWordsFile || require_Q_VectorsFile || require_K_VectorsFile || require_V_VectorsFile) {
            getLogger().info("[MAIC] トレーニングのために、" + AI_BASEDATA_FILE_NAME + "をロードしています。");

            // データのロード
            BaseDataLoader.loadBaseData();
            databaseNodeLength = dataBaseNodeList.getLength();

            // 語彙リストの作成
            if (requireWordsFile) {
                getLogger().info("[MAIC] 語彙リストを作成します。。");
                WordsCreater.main();
            }

        }


    }
}

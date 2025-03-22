package com.makkii.maic.file_manager;

import java.io.File;
import java.io.IOException;

import static com.makkii.maic.MainAI.mainAI;
import static org.bukkit.Bukkit.getLogger;

public enum FileManager {
    ;

    public static final String AI_DATABASE_FILE_NAME = "ai_database.xml";
    public static final String AI_WORDS_FILE_NAME = "ai_words.txt";
    public static final String AI_VECTORS_FILE_NAME = "ai_vectors.txt";

    public static File aiDatabaseFile;
    public static File aiWordsFile;
    public static File aiVectorsFile;

    public static boolean requireWordsFile;
    public static boolean requireVectorsFile;

    public static void init() throws IOException {

        aiDatabaseFile = new File(mainAI.getDataFolder(), AI_DATABASE_FILE_NAME);
        if (!aiDatabaseFile.exists()) {
            throw new IOException(AI_DATABASE_FILE_NAME + "が見つかりません。");
        }

        aiWordsFile = new File(mainAI.getDataFolder(), AI_WORDS_FILE_NAME);
        if (!aiWordsFile.exists()) {
            String errorMsg = AI_WORDS_FILE_NAME + "の作成に失敗しました。";
            try {
                if (!aiWordsFile.createNewFile()) throw new IOException(errorMsg);
                requireWordsFile = true;  // 作成したファイルは空なので、語彙リストを作成してもらうためのフラグ
            } catch (IOException e) {
                getLogger().warning(errorMsg);
            }
        }

        aiVectorsFile = new File(mainAI.getDataFolder(), AI_VECTORS_FILE_NAME);
        if (!aiVectorsFile.exists()) {
            String errorMsg = AI_VECTORS_FILE_NAME + "の作成に失敗しました。";
            try {
                if (!aiVectorsFile.createNewFile()) throw new IOException(errorMsg);
                requireVectorsFile = true;  // 作成したファイルは空なので、トレーニングをしてもらうためのフラグ
            } catch (IOException e) {
                getLogger().warning(errorMsg);
            }
        }

        getLogger().info("[MAIC] PluginDataFolder: " + mainAI.getDataFolder());
        getLogger().info("[MAIC] DatabaseFileName: " + AI_DATABASE_FILE_NAME);
        getLogger().info("[MAIC] WordsFileName: " + AI_WORDS_FILE_NAME);
        getLogger().info("[MAIC] VectorsFileName: " + AI_VECTORS_FILE_NAME);
    }
}

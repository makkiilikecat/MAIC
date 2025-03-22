package com.makkii.maic.oldAI;

import java.io.File;

import static com.makkii.maic.MainAI.mainAI;
import static org.bukkit.Bukkit.getServer;

public class MainAI {

    public static void onEnable() {
        // config.yml と同じディレクトリに ai_database ファイルがあるか確認
        File aiDatabaseFile = new File(mainAI.getDataFolder(), "ai_database");
        File aiDatabaseJson = new File(mainAI.getDataFolder(), "ai_database.json");

        //TODO: もし ai_database がディレクトリなら、その中のファイルリストを処理

        if (!aiDatabaseJson.exists() && aiDatabaseFile.exists()) {
            // JSONファイルが存在せず、ai_databaseファイルが存在する場合、変換処理
            // 今回は、ai_database.txtを直接利用するため、変換処理は不要
        }

        // データの読み込み
        DataLoader dataLoader = new DataLoader();
        // config.ymlと同じディレクトリのai_database.txtを指定
        dataLoader.loadData(new File(mainAI.getDataFolder(), "ai_database.txt").getAbsolutePath());

        // n-gramモデルの初期化 (ここでは2-gramを使用。使わないが、クラスの引数に必要なため)
        NGramModel model = new NGramModel(2, dataLoader);

        // チャットリスナーの登録
        getServer().getPluginManager().registerEvents(new ChatListener(model), mainAI);
    }

    public static void onDisable() {
        // プラグインが無効化されるときの処理（必要に応じて）
    }
}
/*package com.makkii.maic.oldAI.newAI;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public class MainAI extends JavaPlugin {

    public static Transformer transformer;
    public static BPE bpe;
    public static Word2Vec word2Vec;
    public static MainAI mainAI;

    @Override
    public  void onEnable() {
        mainAI = this;

        try {
            saveDefaultConfig();
        } catch (Exception e) {
                getLogger().warning("初期設定のコンフィグをセーブできませんでした。");
        }

        // 2. 各種クラスのインスタンス化
        try {
            bpe = new BPE(getDataFolder().getAbsolutePath());
            word2Vec = new Word2Vec(getDataFolder().getAbsolutePath());
            transformer = new Transformer(getDataFolder().getAbsolutePath());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "[MAIC] 初期化1中にエラーが発生しました。", e);
            getServer().getPluginManager().disablePlugin(mainAI); // プラグインを無効化
            return;
        }

        // 3. "ai_database.json"と"words.json"の生成 (必要に応じて)
        try {
            bpe.createAiDatabaseJson();
            bpe.createWordsJson();
            word2Vec.loadOrTrainModel();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "[MAIC] 初期化2中にエラーが発生しました。", e);
            getServer().getPluginManager().disablePlugin(mainAI); // プラグインを無効化
            return;
        }

        // 4. コマンドの登録
        getCommand("ai").setExecutor(new AICommand(transformer));

        getLogger().info("MAIC Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MAIC Plugin has been disabled.");
    }
}*/
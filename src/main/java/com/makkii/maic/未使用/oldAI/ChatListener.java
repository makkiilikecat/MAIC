package com.makkii.maic.oldAI;

import com.makkii.maic.MainAI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public class ChatListener implements Listener {

    private final NGramModel model;

    public ChatListener(NGramModel model) {
        this.model = model;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        getLogger().info("message: " + message);
        List<String> context = preprocessAndTokenize(message); // メッセージの前処理とトークン化

        //最低限の文字数(n-gram数)に満たない場合、処理をしない
        if (context.size() < model.getN() - 1) {
            return;
        }

        String nextWord = model.predictNextWord(context);
        getLogger().info("nexword: " + nextWord);

        // 非同期処理（応答が遅い場合）
        Bukkit.getScheduler().runTask(MainAI.getPlugin(MainAI.class), () -> {
            event.getPlayer().sendMessage("[oldAI] " + nextWord);
        });
    }

    private List<String> preprocessAndTokenize(String message) {
        // DataLoaderのpreprocessとtokenizeを再利用
        DataLoader loader = new DataLoader(); // 本来は毎回インスタンス化しない方が良い
        getLogger().info("rawMessage: " + message);
        String preprocessed = Util.preprocess(message);
        getLogger().info("preprocessed: " + preprocessed);
        return Util.tokenize(preprocessed);
    }
}
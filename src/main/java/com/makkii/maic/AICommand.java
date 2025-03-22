package com.makkii.maic;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AICommand implements CommandExecutor {

    private static final long COOLDOWN_MILLIS = 5000L; // 5秒のクールダウン (ミリ秒)
    private static final UUID CONSOLEUUID = UUID.randomUUID();
    private final Map<UUID, Long> lastCommandTime = new HashMap<>(); // 最後にコマンドを実行した時間

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        UUID senderUUID;
        if (!(sender instanceof Player)) {
            senderUUID = ((Player) sender).getPlayer().getUniqueId();
        } else {
            senderUUID = CONSOLEUUID;
        }

        // クールダウンチェック
        if (lastCommandTime.containsKey(senderUUID)) {
            long timeSinceLastCommand = System.currentTimeMillis() - lastCommandTime.get(senderUUID);
            if (timeSinceLastCommand < COOLDOWN_MILLIS) {
                long timeLeft = (COOLDOWN_MILLIS - timeSinceLastCommand) / 1000L;
                sender.sendMessage("§cクールダウン中です。あと" + timeLeft + "秒お待ちください。");
                return true;
            }
        }

        // 推論中かどうかのチェック
        if (MainAI.isGeneratingFlag(senderUUID)) {
            sender.sendMessage("§c前の応答を生成中です。しばらくお待ちください。");
            return true;
        }

        // プロンプトの取得
        if (args.length == 0) {
            sender.sendMessage("§cプロンプトを入力してください: /ai <プロンプト>");
            return true;
        }
        String prompt = String.join(" ", args);

        // 最後のコマンド実行時間を記録
        lastCommandTime.put(senderUUID, System.currentTimeMillis());

        // 非同期で推論処理を実行
        //CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
        //    String[] tokens = KuromojiTokenizer.tokenize(prompt);
        //    return TextGenerator.generateText(tokens, senderUUID);
        //});

        // 推論完了後にチャットに応答を送信
        //futureResponse.thenAcceptAsync(response -> {
        //    getServer().getScheduler().runTask(mainAI, () -> { // Bukkitのスケジューラを使用
        //        sender.sendMessage("§aAI: §f" + response);
        //        // 完了したら、推論中フラグをfalseにする (TransformerクラスにremoveGeneratingPlayerメソッドを後で追加)
        //        MainAI.setGeneratingFlag(senderUUID, false);
        //    });
        //});

        // 推論開始メッセージを送信
        sender.sendMessage("§aAIが応答を生成中です...");

        return true;
    }
}
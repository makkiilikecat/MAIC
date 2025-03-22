package com.makkii.maic.oldAI.newAI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.makkii.maic.MainAI.mainAI;

public class AICommand implements CommandExecutor {

    private final Transformer transformer;
    private final Map<UUID, Long> lastCommandTime = new HashMap<>(); // 最後にコマンドを実行した時間
    private final long COOLDOWN_MILLIS = 5000; // 5秒のクールダウン (ミリ秒)

    public AICommand(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーからのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // クールダウンチェック
        if (lastCommandTime.containsKey(playerId)) {
            long timeSinceLastCommand = System.currentTimeMillis() - lastCommandTime.get(playerId);
            if (timeSinceLastCommand < COOLDOWN_MILLIS) {
                long timeLeft = (COOLDOWN_MILLIS - timeSinceLastCommand) / 1000;
                player.sendMessage("§cクールダウン中です。あと" + timeLeft + "秒お待ちください。");
                return true;
            }
        }

        // 推論中かどうかのチェック
        if (transformer.isGenerating(playerId)) { // TransformerクラスにisGeneratingメソッドを後で追加
            player.sendMessage("§c現在、別の応答を生成中です。しばらくお待ちください。");
            return true;
        }

        // プロンプトの取得
        if (args.length == 0) {
            player.sendMessage("§cプロンプトを入力してください: /ai <プロンプト>");
            return true;
        }
        String prompt = String.join(" ", args);

        // 最後のコマンド実行時間を記録
        lastCommandTime.put(playerId, System.currentTimeMillis());

        // 非同期で推論処理を実行
        CompletableFuture<String> futureResponse = CompletableFuture.supplyAsync(() -> {
            try {
                return transformer.generateText(player, prompt, 200); // 最大200文字生成
            } catch (IOException e) {
                e.printStackTrace();
                return "§cエラーが発生しました: " + e.getMessage();
            }
        });

        // 推論完了後にチャットに応答を送信
        futureResponse.thenAcceptAsync(response -> {
            mainAI.getServer().getScheduler().runTask(mainAI, () -> { // Bukkitのスケジューラを使用
                player.sendMessage("§aAI: §f" + response);
                // 完了したら、推論中フラグをfalseにする (TransformerクラスにremoveGeneratingPlayerメソッドを後で追加)
                transformer.removeGeneratingPlayer(playerId);
            });
        });

        // 推論開始メッセージを送信
        player.sendMessage("§aAIが応答を生成中です...");

        return true;
    }
}
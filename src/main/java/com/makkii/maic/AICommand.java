package com.makkii.maic;

import com.makkii.maic.file_manager.SentenceLabeler;
import com.makkii.maic.file_manager.words.WordsLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AICommand implements CommandExecutor {

    private static final long COOLDOWN_MILLIS = 5000L; // 5秒のクールダウン (ミリ秒)
    private static final UUID CONSOLEUUID = UUID.randomUUID();
    private final Map<UUID, Long> lastCommandTime = new HashMap<>(); // 最後にコマンドを実行した時間

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        UUID senderUUID;
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
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
        String prompt = SentenceLabeler.WHITESPACE_PATTERN.matcher(String.join("", args).toLowerCase()).replaceAll("");
        sender.sendMessage("prompt: " + prompt);

        // 最後のコマンド実行時間を記録
        lastCommandTime.put(senderUUID, System.currentTimeMillis());

        // 推論開始メッセージを送信
        sender.sendMessage("§aAIが応答を生成中です...");

        ArrayList<String> tokens = KuromojiTokenizer.promptTokenize(prompt);
        List<Integer> tokenIndices = WordsLoader.convertToIndexList(tokens);
        tokenIndices = tokenIndices.stream()
                .filter(index -> index != -1)
                .collect(Collectors.toList());

        sender.sendMessage("tokendWord: " + WordsLoader.convertToWordList(tokenIndices));
        double[] response = TextGenerator.generateText(tokenIndices, senderUUID);
        String responseText = WordsLoader.getWord(TextGenerator.arraysToWordId(response));
        sender.sendMessage("response: " + responseText);


        return true;
    }
}
package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.back.BackListener;
import com.nekoessentialsx.back.BackManager;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * /back 命令
 * 回到最近一次的传送/死亡前的位置。仅支持最近一次，使用后清除记录。
 */
public class Commandback implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final BackManager backManager;
    private final BackListener backListener;

    public Commandback(NekoEssentialX plugin, BackManager backManager, BackListener backListener) {
        this.plugin = plugin;
        this.backManager = backManager;
        this.backListener = backListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c呜...只有玩家才可以用这个命令的说~喵~");
                return true;
            }

            Player player = (Player) sender;
            CatChatProcessor processor = CatChatProcessor.getInstance();

            if (!backManager.has(player)) {
                String message = "§c呜...你现在没有可以回去的位置的说（需要先传送或死亡一次）喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            Location target = backManager.get(player);
            if (target == null || target.getWorld() == null) {
                String message = "§c呜...要回去的位置不在有效世界里啦喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                backManager.clear(player);
                return true;
            }

            // 记录：/back 使用后清除，仅支持一次
            backManager.clear(player);

            // 标记，防止/back自身传送被记录成新的回退点
            backListener.beginBack(player);
            try {
                player.teleport(target);
            } finally {
                backListener.endBack(player);
            }

            String message = "§a呜呼~回到了之前的那个地方喵~（坐标: §e" + String.format("%.0f,%.0f,%.0f", target.getX(), target.getY(), target.getZ()) + "§a）";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
        } catch (Exception e) {
            String errorMessage = "§c呜哇...执行命令的时候出错啦！米娜对不起了喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, errorMessage);
                } else {
                    sender.sendMessage(errorMessage);
                }
            } else {
                sender.sendMessage(errorMessage);
            }
            plugin.getLogger().severe("执行back命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // back命令不需要Tab补全，返回空列表
        return List.of();
    }
}
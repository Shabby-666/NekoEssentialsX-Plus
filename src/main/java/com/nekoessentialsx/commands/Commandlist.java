package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Commandlist implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;

    public Commandlist(NekoEssentialX plugin) {
        this.plugin = plugin;
        // 注册Tab补全器到list命令及其所有别名
        plugin.getCommand("list").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            CatChatProcessor processor = CatChatProcessor.getInstance();
            
            // 获取在线玩家列表
            List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
            int onlineCount = onlinePlayers.size();
            int maxPlayers = plugin.getServer().getMaxPlayers();
            
            // 显示在线玩家统计信息
            String statsMessage = "§a呜~现在在线的主人: §6" + onlineCount + "§a/§6" + maxPlayers + " §a 的说~喵~";
            
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, statsMessage);
            } else {
                sender.sendMessage(statsMessage);
            }
            
            // 显示在线玩家名称列表
            if (onlinePlayers.isEmpty()) {
                String emptyMessage = "§c呜...现在一个在线的玩家都没有的说~好寂寞喵~";
                if (sender instanceof Player && processor != null) {
                    processor.sendCatStyleMessage((Player) sender, emptyMessage);
                } else {
                    sender.sendMessage(emptyMessage);
                }
            } else {
                // 按名称排序
                Collections.sort(onlinePlayers, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                
                // 构建玩家列表字符串
                String playerList = onlinePlayers.stream()
                        .map(Player::getName)
                        .collect(Collectors.joining("§a, §f"));
                
                String listMessage = "§f在线的玩家有: §a" + playerList + " §f 的说~喵~";
                if (sender instanceof Player && processor != null) {
                    processor.sendCatStyleMessage((Player) sender, listMessage);
                } else {
                    sender.sendMessage(listMessage);
                }
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
            plugin.getLogger().severe("执行list命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // list命令不需要参数，返回空列表
        return Collections.emptyList();
    }
}
package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import java.util.Collections;
import java.util.List;

public class Commandinfo implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;

    public Commandinfo(NekoEssentialX plugin) {
        this.plugin = plugin;
        // 注册Tab补全器到info命令及其所有别名
        plugin.getCommand("info").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            CatChatProcessor processor = CatChatProcessor.getInstance();
            PluginDescriptionFile pdf = plugin.getDescription();
            
            // 显示NekoEssentialX信息
            String[] messages = {
                "§6===== §eNekoEssentialsX+ 插件信息 §6=====",
                "§a插件名称: §f" + pdf.getName(),
                "§a版本: §f" + pdf.getVersion(),
                "§a作者: §f" + String.join(", ", pdf.getAuthors()),
                "§a描述: §f" + pdf.getDescription(),
                "§a主命令: §f/nekoessentialx",
                "§a依赖: §f" + String.join(", ", pdf.getDepend()),
                "§a软依赖: §f" + String.join(", ", pdf.getSoftDepend()),
                "§a服务器版本: §f" + plugin.getServer().getVersion(),
                "§a在线玩家: §f" + plugin.getServer().getOnlinePlayers().size() + "/" + plugin.getServer().getMaxPlayers(),
                "§6==================================="
            };

            for (String message : messages) {
                String catMessage = message + "喵~";
                if (sender instanceof Player && processor != null) {
                    processor.sendCatStyleMessage((Player) sender, catMessage);
                } else {
                    sender.sendMessage(catMessage);
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
            plugin.getLogger().severe("执行info命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // info命令不需要参数，返回空列表
        return Collections.emptyList();
    }
}
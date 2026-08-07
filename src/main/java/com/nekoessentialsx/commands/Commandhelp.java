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

public class Commandhelp implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;

    public Commandhelp(NekoEssentialX plugin) {
        this.plugin = plugin;
        // 注意：/help是服务器内置命令，不能直接获取和设置Tab补全器
        // 此构造函数仅用于初始化插件引用
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            CatChatProcessor processor = CatChatProcessor.getInstance();
            
            // 显示NekoEssentialX帮助信息
            String[] messages = {
                "§6===== §eNekoEssentialsX+ 帮助中心 §6=====",
                "§a/nekoessentialx §7- 查看插件主命令",
                "§a/etitle §7- 管理头衔",
                "§a/money §7- 管理经济",
                "§a/titlegui §7- 打开头衔GUI",
                "§a/tpa <玩家> §7- 请求传送到指定玩家",
                "§a/tpaccept [玩家|*] §7- 接受传送请求",
                "§a/tpdeny [玩家|*] §7- 拒绝传送请求",
                "§a/tpacancel [玩家] §7- 取消传送请求",
                "§a/nekoessentialx checkin §7- 每日签到",
                "§a/nekoessentialx claimgift §7- 领取新手礼包",
                "§a/help §7- 查看此帮助信息",
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
            plugin.getLogger().severe("执行help命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 补全插件相关命令
            String[] pluginCommands = {
                "nekoessentialx", "etitle", "money", "titlegui", 
                "tpa", "tpaccept", "tpdeny", "tpacancel"
            };
            for (String cmd : pluginCommands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
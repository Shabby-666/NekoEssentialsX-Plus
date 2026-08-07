package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.titles.TitleManager;
import com.nekoessentialsx.titles.TitleManager.Title;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class Commandmsg implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final TitleManager titleManager;

    public Commandmsg(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        // 注册Tab补全器到msg命令及其别名
        plugin.getCommand("msg").setTabCompleter(this);
        plugin.getCommand("tell").setTabCompleter(this);
        plugin.getCommand("whisper").setTabCompleter(this);
        plugin.getCommand("w").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length < 2) {
                String message = "§c呜...正确的用法是这样哦: /" + label + " <玩家|@选择器> <消息> 喵~";
                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, message);
                    } else {
                        sender.sendMessage(message);
                    }
                } else {
                    sender.sendMessage(message);
                }
                return true;
            }

            String targetName = args[0];
            java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, targetName);
            
            if (targets.isEmpty()) {
                String message = "§c呜...找不到玩家 §e" + targetName + "§c 的说~喵~";
                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, message);
                    } else {
                        sender.sendMessage(message);
                    }
                } else {
                    sender.sendMessage(message);
                }
                return true;
            }

            // 构建消息
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String rawMessage = messageBuilder.toString().trim();
            
            // 转换消息为猫娘风格
            String message = rawMessage;
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    message = processor.convertToCatStyle(rawMessage);
                    // 确保消息以喵~结尾
                    if (!message.endsWith("喵~")) {
                        message += "§6喵~";
                    }
                }
            }

            // 发送消息给每个目标玩家
            String senderDisplayName = sender instanceof Player ? getPlayerDisplayName((Player) sender) : "控制台";
            String targetDisplayName = getPlayerDisplayName(targets.get(0));
            
            String senderPrefix = sender instanceof Player ? "§a[我 -> §b" + targetDisplayName + "§a]§f " : "§c[控制台 -> §b" + targetDisplayName + "§c]§f ";
            String targetPrefix = "§a[§b" + senderDisplayName + " §a-> 我]§f ";
            
            // 发送给发送者
            sender.sendMessage(senderPrefix + message);
            
            // 发送给接收者
            for (Player target : targets) {
                target.sendMessage(targetPrefix + message);
                // 保存最近的消息对象，用于/r命令回复
                if (sender instanceof Player) {
                    Commandr.setLastMessageSender(target, (Player) sender);
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
            plugin.getLogger().severe("执行msg命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * 获取带头衔的玩家显示名称
     * @param player 玩家
     * @return 带头衔的显示名称
     */
    private String getPlayerDisplayName(Player player) {
        String playerName = player.getName();
        
        // 如果头衔管理器已启用，获取玩家的头衔
        if (titleManager != null && titleManager.isEnabled()) {
            String titleId = titleManager.getPlayerTitle(playerName);
            if (titleId != null) {
                // 检查是否为系统头衔
                Title title = titleManager.getTitle(titleId);
                if (title != null) {
                    // 系统头衔，返回前缀+玩家名
                    return title.getPrefix() + playerName;
                } else {
                    // 自定义头衔，从数据库获取
                    Object[] customTitleData = plugin.getDatabaseManager().getCustomTitle(titleId);
                    if (customTitleData != null) {
                        String prefix = (String) customTitleData[1];
                        return prefix + playerName;
                    }
                }
            }
        }
        
        // 如果没有头衔或头衔管理器未启用，返回玩家名
        return playerName;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 补全在线玩家名称与选择器
            completions = com.nekoessentialsx.util.PlayerSelector.complete(sender, args[0]);
        }
        
        return completions;
    }
}
package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.titles.TitleManager;
import com.nekoessentialsx.titles.TitleManager.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commandr implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final TitleManager titleManager;
    private static final Map<Player, Player> lastMessageSenders = new HashMap<>();

    public Commandr(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        // 注册Tab补全器到r命令及其别名
        plugin.getCommand("r").setTabCompleter(this);
        plugin.getCommand("reply").setTabCompleter(this);
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
            
            if (args.length < 1) {
                String message = "§c呜...正确的用法是这样哦: /" + label + " <消息> 喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            Player lastSender = lastMessageSenders.get(player);
            
            if (lastSender == null || !lastSender.isOnline()) {
                String message = "§c呜...你还没有可以回复的消息的说~喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            // 构建消息
            StringBuilder messageBuilder = new StringBuilder();
            for (String arg : args) {
                messageBuilder.append(arg).append(" ");
            }
            String rawMessage = messageBuilder.toString().trim();
            
            // 转换消息为猫娘风格
            String message = rawMessage;
            if (processor != null) {
                message = processor.convertToCatStyle(rawMessage);
                // 确保消息以喵~结尾
                if (!message.endsWith("喵~")) {
                    message += "§6喵~";
                }
            }

            // 发送消息
            String senderDisplayName = getPlayerDisplayName(player);
            String lastSenderDisplayName = getPlayerDisplayName(lastSender);
            
            String senderPrefix = "§a[我 -> §b" + lastSenderDisplayName + "§a]§f ";
            String targetPrefix = "§a[§b" + senderDisplayName + " §a-> 我]§f ";
            
            // 发送给发送者
            player.sendMessage(senderPrefix + message);
            
            // 发送给接收者
            lastSender.sendMessage(targetPrefix + message);
            
            // 更新最近的消息发送者
            setLastMessageSender(lastSender, player);
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
            plugin.getLogger().severe("执行r命令时发生错误: " + e.getMessage());
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
        // r命令不需要Tab补全，返回空列表
        return List.of();
    }

    /**
     * 设置玩家的最近消息发送者
     * @param receiver 消息接收者
     * @param sender 消息发送者
     */
    public static void setLastMessageSender(Player receiver, Player sender) {
        lastMessageSenders.put(receiver, sender);
    }

    /**
     * 移除玩家的最近消息发送者记录
     * @param player 要移除记录的玩家
     */
    public static void removeLastMessageSender(Player player) {
        lastMessageSenders.remove(player);
    }
}
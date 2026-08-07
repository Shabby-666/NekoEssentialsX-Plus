package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.warp.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class Commanddelwarp implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final WarpManager warpManager;

    public Commanddelwarp(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.warpManager = plugin.getWarpManager();
        // 注册Tab补全器到delwarp命令
        plugin.getCommand("delwarp").setTabCompleter(this);
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
            
            // 检查权限
            if (!player.hasPermission("nekoessentialsx.warp.admin")) {
                String message = "§c呜...主人没有权限用这个命令的说~喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            if (args.length < 1) {
                String message = "§c呜...正确的用法是这样哦: /delwarp <传送点名称> 喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            String warpName = args[0];
            
            // 检查传送点是否存在
            if (!warpManager.warpExists(warpName)) {
                String message = "§c呜...没有找到名叫 '" + warpName + "' 的传送点，不用删的说喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
                return true;
            }
            
            // 删除传送点
            boolean success = warpManager.deleteWarp(warpName);
            
            if (success) {
                String message = "§a呜呼~传送点 '§6" + warpName + "§a' 删掉啦喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
            } else {
                String message = "§c呜...删除传送点失败了的说~喵~";
                if (processor != null) {
                    processor.sendCatStyleMessage(player, message);
                } else {
                    player.sendMessage(message);
                }
            }
        } catch (Exception e) {
            String errorMessage = "§c呜哇...执行命令的时候出错啦！主人对不起了喵~";
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
            plugin.getLogger().severe("执行delwarp命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partialName = args[0];
            List<String> warpNames = warpManager.getWarpNames();
            
            for (String warpName : warpNames) {
                if (warpName.startsWith(partialName)) {
                    completions.add(warpName);
                }
            }
        }
        
        return completions;
    }
}
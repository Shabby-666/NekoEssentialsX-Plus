package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.gui.ChestGUIManager;
import com.nekoessentialsx.warp.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * 传送点命令
 * 支持双调用方式：指令调用和GUI调用
 */
public class Commandwarp implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final WarpManager warpManager;
    private final ChestGUIManager chestGUIManager;

    public Commandwarp(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.warpManager = plugin.getWarpManager();
        this.chestGUIManager = plugin.getChestGUIManager();
        // 注册Tab补全器到warp命令
        plugin.getCommand("warp").setTabCompleter(this);
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
                // 如果没有提供传送点名称，打开传送点菜单GUI
                chestGUIManager.openWarpMenu(player, 1);
                return true;
            }

            String warpName = args[0];
            teleportToWarp(player, warpName, processor);
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
            plugin.getLogger().severe("执行warp命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 传送到传送点
     * 此方法同时被指令和GUI调用
     * @param player 玩家
     * @param warpName 传送点名称
     * @param processor 猫娘风格处理器
     */
    public void teleportToWarp(Player player, String warpName, CatChatProcessor processor) {
        // 检查传送点是否存在
        if (!warpManager.warpExists(warpName)) {
            String message = "§c呜...找不到名叫 '" + warpName + "' 的传送点哦~喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
            return;
        }
        
        // 传送到传送点
        boolean success = warpManager.teleportToWarp(player, warpName);
        
        if (success) {
            String message = "§a呜呼~唰一下就传送到传送点 '§6" + warpName + "§a' 啦喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
        } else {
            String message = "§c呜...传送失败了，检查一下传送点是不是还有效的说喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
        }
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

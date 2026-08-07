package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.gui.ChestGUIManager;
import com.nekoessentialsx.kits.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具包命令
 * 支持双调用方式：指令调用和GUI调用
 */
public class Commandkit implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final KitManager kitManager;
    private final ChestGUIManager chestGUIManager;

    public Commandkit(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
        this.chestGUIManager = plugin.getChestGUIManager();
        // 注册Tab补全器到kit命令
        plugin.getCommand("kit").setTabCompleter(this);
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
                // 如果没有提供工具包名称，打开工具包菜单GUI
                chestGUIManager.openKitMenu(player, 1);
                return true;
            }

            String kitName = args[0];
            claimKit(player, kitName, processor);
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
            plugin.getLogger().severe("执行kit命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 领取工具包
     * 此方法同时被指令和GUI调用
     * @param player 玩家
     * @param kitName 工具包名称
     * @param processor 猫娘风格处理器
     */
    public void claimKit(Player player, String kitName, CatChatProcessor processor) {
        // 检查工具包是否存在
        if (!kitManager.getKitNames().contains(kitName)) {
String message = "§c呜...找不到名叫 '" + kitName + "' 的工具包的说~喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
            return;
        }
        
        // 领取工具包
        kitManager.giveKit(player, kitName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (sender instanceof Player && args.length == 1) {
            Player player = (Player) sender;
            List<String> kitNames = kitManager.getKitNames();
            String partialName = args[0];
            
            for (String kitName : kitNames) {
                if (kitName.startsWith(partialName)) {
                    // 只显示玩家可以领取的工具包
                    if (kitManager.canClaimKit(player, kitName) == null) {
                        completions.add(kitName);
                    }
                }
            }
        }
        
        return completions;
    }
}

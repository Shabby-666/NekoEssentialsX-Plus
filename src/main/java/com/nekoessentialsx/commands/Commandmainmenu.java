package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.gui.ChestGUIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;

/**
 * 主菜单命令
 * 支持通过指令打开箱子GUI主菜单
 */
public class Commandmainmenu implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final ChestGUIManager chestGUIManager;

    public Commandmainmenu(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.chestGUIManager = plugin.getChestGUIManager();
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
            
            // 打开箱子GUI主菜单
            chestGUIManager.openMainMenu(player);
            
            // 发送猫娘风格的消息
            if (processor != null) {
                processor.sendCatStyleMessage(player, "§a嘿嘿~这就为主人打开主菜单的说~喵~");
            } else {
                player.sendMessage("§a嘿嘿~这就为主人打开主菜单的说~喵~");
            }
            
            plugin.getLogger().info("玩家 " + player.getName() + " 打开了主菜单！喵~");
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
            plugin.getLogger().severe("执行mainmenu命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // mainmenu命令不需要Tab补全，返回空列表
        return List.of();
    }
}

package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.database.DatabaseManager;
import com.nekoessentialsx.gui.ChestGUIManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

/**
 * 家命令
 * 支持双调用方式：指令调用和GUI调用
 */
public class Commandhome implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private final ChestGUIManager chestGUIManager;

    public Commandhome(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.chestGUIManager = plugin.getChestGUIManager();
        // 注册Tab补全器到home命令
        plugin.getCommand("home").setTabCompleter(this);
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
            String playerId = player.getName();
            
            if (args.length > 0) {
                String homeName = args[0];
                teleportToHome(player, homeName, processor);
            } else {
                // 如果没有提供家名称，打开家菜单GUI
                chestGUIManager.openHomeMenu(player);
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
            plugin.getLogger().severe("执行home命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 传送到指定家
     * 此方法同时被指令和GUI调用
     * @param player 玩家
     * @param homeName 家名称
     * @param processor 猫娘风格处理器
     */
    public void teleportToHome(Player player, String homeName, CatChatProcessor processor) {
        String playerId = player.getName();
        
        if (!databaseManager.hasPlayerHome(playerId, homeName)) {
            String message = "§c呜...你还没有名叫 '" + homeName + "' 的家哦~先用 /sethome 设置一个的说喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
            return;
        }

        // 获取家的位置信息
        Object[] homeInfo = databaseManager.getPlayerHome(playerId, homeName);
        if (homeInfo == null) {
            String message = "§c呜...找不到家位置的说，是不是被谁弄丢了喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
            return;
        }

        // 解析位置信息
        String worldName = (String) homeInfo[0];
        double x = (double) homeInfo[1];
        double y = (double) homeInfo[2];
        double z = (double) homeInfo[3];
        float yaw = (float) homeInfo[4];
        float pitch = (float) homeInfo[5];

        // 获取世界对象
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            String message = "§c呜...找不到对应世界的说~那个世界是不是不存在啦喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                player.sendMessage(message);
            }
            return;
        }

        // 创建位置对象
        Location homeLocation = new Location(world, x, y, z, yaw, pitch);

        // 传送玩家到家中
        player.teleport(homeLocation);

        // 发送成功消息
        String message = "§a呜呼~唰一下回到家 '" + homeName + "' 啦！欢迎回来的喵~";
        if (processor != null) {
            processor.sendCatStyleMessage(player, message);
        } else {
            player.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (args.length == 1) {
            // 补全已有的家名称
            return databaseManager.getPlayerHomeNames(player.getName());
        }
        return Collections.emptyList();
    }
}

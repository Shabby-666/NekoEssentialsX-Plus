package com.nekoessentialsx.dailylogin;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import com.nekoessentialsx.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DailyLoginManager {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private boolean enabled = false;

    public DailyLoginManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
    }

    /**
     * 启用每日登录管理器
     */
    public void onEnable() {
        enabled = true;
        plugin.getLogger().info("每日登录管理器已启用！喵~");
    }

    /**
     * 禁用每日登录管理器
     */
    public void onDisable() {
        enabled = false;
        plugin.getLogger().info("每日登录管理器已禁用！喵~");
    }

    /**
     * 处理玩家登录
     * @param player 玩家对象
     */
    public void handlePlayerLogin(Player player) {
        if (!enabled) {
            return;
        }

        String playerName = player.getName();
        LocalDate today = LocalDate.now();
        
        // 获取玩家上次登录日期
        Date lastLoginDate = databaseManager.getPlayerLastLogin(playerName);
        LocalDate lastLoginLocalDate = null;
        if (lastLoginDate != null) {
            lastLoginLocalDate = lastLoginDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        
        // 检查是否为新的一天登录
        boolean isNewDay = lastLoginLocalDate == null || !today.equals(lastLoginLocalDate);
        
        // 检查是否为首次登录
        boolean isFirstLogin = lastLoginDate == null;
        
        // 更新玩家登录时间
        databaseManager.updatePlayerLastLogin(playerName, new Date());
        
        // 如果是新的一天登录，更新累计登录天数
        if (isNewDay) {
            int loginDays = databaseManager.getPlayerLoginDays(playerName) + 1;
            databaseManager.setPlayerLoginDays(playerName, loginDays);
        }
        
        // 获取累计登录天数
        int totalLoginDays = databaseManager.getPlayerLoginDays(playerName);
        
        // 检查玩家今天是否已经签到
        boolean hasCheckedIn = hasCheckedInToday(playerName);
        
        // 发送登录欢迎消息，使用MiniMessage格式
        try {
            // 使用Bukkit的Component API发送带有点击事件的消息
            player.sendMessage("§a欢迎回来的说~§b" + playerName + "§a桑！服务器已经陪你度过了§e" + totalLoginDays + "§a天的说~呜呼，好开心喵~");
            
            // 如果没有签到，发送签到提醒
            if (!hasCheckedIn && !isNewPlayer(playerName)) {
                // 创建可点击的签到按钮
                net.md_5.bungee.api.chat.TextComponent checkinBtn = new net.md_5.bungee.api.chat.TextComponent("[每日签到]");
                checkinBtn.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                checkinBtn.setBold(true);
                checkinBtn.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nekoessentialx checkin"
                ));
                checkinBtn.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                    new net.md_5.bungee.api.chat.TextComponent[]{
                        new net.md_5.bungee.api.chat.TextComponent("§a点击领取今日签到奖励的说~")
                    }
                ));
                
                // 创建后续文本
                net.md_5.bungee.api.chat.TextComponent afterText = new net.md_5.bungee.api.chat.TextComponent(" §7点击领取今日奖励的说~喵~");
                
                // 组合消息并发送
                player.spigot().sendMessage(checkinBtn, afterText);
                player.sendMessage("§7使用命令 §a/nekoessentialx checkin §7进行每日签到的说~");
            }
            
            plugin.getLogger().info("发送登录欢迎消息给玩家 " + playerName + "！喵~");
        } catch (Exception e) {
            // 回退到普通消息格式
            player.sendMessage("§a欢迎回来的说~" + playerName + "桑！服务器已经陪你度过了" + totalLoginDays + "天了的说~喵~");
            if (!hasCheckedIn && !isNewPlayer(playerName)) {
                player.sendMessage("§a[每日签到] §7点击领取今日奖励的说~喵~");
            }
            plugin.getLogger().warning("发送登录欢迎消息失败，已回退到普通格式: " + e.getMessage());
        }
        
        plugin.getLogger().info("玩家 " + playerName + " 登录，累计登录天数: " + totalLoginDays + " 天！喵~");
    }

    /**
     * 检查玩家今天是否已经签到
     * @param playerName 玩家名称
     * @return 是否已签到
     */
    public boolean hasCheckedInToday(String playerName) {
        LocalDate today = LocalDate.now();
        
        // 获取玩家上次签到日期
        Date lastCheckInDate = databaseManager.getPlayerLastCheckIn(playerName);
        if (lastCheckInDate == null) {
            return false;
        }
        
        LocalDate lastCheckInLocalDate = lastCheckInDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return today.equals(lastCheckInLocalDate);
    }
    
    /**
     * 获取玩家的累计登录天数
     * @param playerName 玩家名称
     * @return 累计登录天数
     */
    public int getPlayerLoginDays(String playerName) {
        return databaseManager.getPlayerLoginDays(playerName);
    }
    
    /**
     * 获取管理器是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 检查玩家是否是新玩家
     * @param playerName 玩家名称
     * @return 是否是新玩家
     */
    private boolean isNewPlayer(String playerName) {
        // 新玩家的判断：累计登录天数为0
        return databaseManager.getPlayerLoginDays(playerName) == 0;
    }
    
    /**
     * 处理玩家每日签到
     * @param player 玩家对象
     */
    public void handleDailyCheckIn(Player player) {
        if (!enabled) {
            return;
        }

        String playerName = player.getName();
        LocalDate today = LocalDate.now();
        
        // 检查是否为新玩家
        if (isNewPlayer(playerName)) {
            player.sendMessage("§c呜...新玩家暂时还不能签到的说，先去把新手礼包领了的喵~");
            return;
        }
        
        // 获取玩家上次签到日期
        Date lastCheckInDate = databaseManager.getPlayerLastCheckIn(playerName);
        LocalDate lastCheckInLocalDate = null;
        if (lastCheckInDate != null) {
            lastCheckInLocalDate = lastCheckInDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        
        // 检查是否已经签到过
        if (lastCheckInLocalDate != null && today.equals(lastCheckInLocalDate)) {
            player.sendMessage("§c呜...你今天已经签到过啦，不要再调皮了嘛喵~");
            return;
        }
        
        // 1. 首先更新玩家签到时间（确保原子性，避免服务器崩溃导致重复签到）
        Date now = new Date();
        if (!databaseManager.updatePlayerLastCheckIn(playerName, now)) {
            player.sendMessage("§c呜...签到失败了的说，请找管理员大大帮忙看看的说~喵~");
            plugin.getLogger().severe("玩家 " + playerName + " 每日签到失败，更新签到时间失败！喵~");
            return;
        }
        
        // 2. 然后发放1单位Vault货币
        double amount = 1.0;
        if (economyManager.depositPlayer(player, amount)) {
            // 发送成功消息
            String currencyName = economyManager.getCurrencyName();
            player.sendMessage("§a签到成功了的说~1" + currencyName + "已经到账啦，呜呼~喵~");
            
            plugin.getLogger().info("玩家 " + playerName + " 每日签到成功，获得了1" + currencyName + "！喵~");
        } else {
            // 发送失败消息
            player.sendMessage("§c呜...签到失败了的说，请找管理员帮忙看看喵~");
            
            plugin.getLogger().severe("玩家 " + playerName + " 每日签到失败，发放奖励失败！喵~");
        }
    }
}
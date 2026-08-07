package com.nekoessentialsx.newbiegift;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NewbieGiftListener implements Listener {
    private final NekoEssentialX plugin;
    private final NewbieGiftManager newbieGiftManager;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Boolean> awaitingGiftClaim = new HashMap<>();

    public NewbieGiftListener(NekoEssentialX plugin, NewbieGiftManager newbieGiftManager) {
        this.plugin = plugin;
        this.newbieGiftManager = newbieGiftManager;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 处理玩家加入事件
     * @param event 玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();
        
        // 处理每日登录
        plugin.getDailyLoginManager().handlePlayerLogin(player);
        
        // 检查玩家是否未领取新手礼包
        if (!databaseManager.hasClaimedNewbieGift(playerName)) {
            // 发送欢迎消息，包含点击领取按钮
            sendWelcomeMessage(player);
        }
    }

    /**
     * 发送欢迎消息
     * @param player 玩家对象
     */
    private void sendWelcomeMessage(Player player) {
        String playerName = player.getName();
        
        try {
            // 发送欢迎消息
            player.sendMessage("§a欢迎新来的猫咪 §b" + playerName + " §a！这是给你的新手大礼包：");
            
            // 创建可点击的领取按钮
            net.md_5.bungee.api.chat.TextComponent claimBtn = new net.md_5.bungee.api.chat.TextComponent("[点击领取]");
            claimBtn.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            claimBtn.setBold(true);
            claimBtn.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nekoessentialx claimgift"
            ));
            claimBtn.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                new net.md_5.bungee.api.chat.TextComponent[]{
                    new net.md_5.bungee.api.chat.TextComponent("§e点击领取你的新手大礼包的说~")
                }
            ));
            
            // 创建后续文本
            net.md_5.bungee.api.chat.TextComponent afterText = new net.md_5.bungee.api.chat.TextComponent(" §a喵~");
            
            // 组合消息并发送
            player.spigot().sendMessage(claimBtn, afterText);
            player.sendMessage("§7用指令 §a/nekoessentialx claimgift §7就能领到新手大礼包的说~喵~");
            
            plugin.getLogger().info("发送欢迎消息给新玩家 " + playerName + "！喵~");
        } catch (Exception e) {
            // 回退到普通消息格式
            player.sendMessage("§a欢迎新来的猫咪 §b" + playerName + " §a！这是给你的新手大礼包：");
            player.sendMessage("§e[点击领取] §a喵~");
            player.sendMessage("§7用指令 §a/nekoessentialx claimgift §7领你的新手大礼包的说~");
            plugin.getLogger().warning("发送欢迎消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家聊天事件，用于检测玩家是否确认领取新手礼包
     * @param event 玩家聊天事件
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 检查玩家是否正在等待领取新手礼包
        if (awaitingGiftClaim.containsKey(playerId) && awaitingGiftClaim.get(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            
            // 检查玩家是否确认领取
            if (message.equalsIgnoreCase("是") || message.equalsIgnoreCase("yes") || message.equalsIgnoreCase("领取")) {
                // 处理新手礼包领取
                newbieGiftManager.handleGiftClaim(player);
                awaitingGiftClaim.remove(playerId);
            } else if (message.equalsIgnoreCase("否") || message.equalsIgnoreCase("no") || message.equalsIgnoreCase("取消")) {
                // 取消领取
                player.sendMessage("§c呜...主人没有打算领新手礼包了吗？喵~");
                awaitingGiftClaim.remove(playerId);
            } else {
                // 提示玩家正确的输入
                player.sendMessage("§e呜...输入 '是' 或 '领取' 就能领新手礼包，输入 '否' 或 '取消' 就不要了的说~喵~");
            }
        }
    }

    /**
     * 设置玩家是否正在等待领取新手礼包
     * @param player 玩家对象
     * @param awaiting 是否等待
     */
    public void setAwaitingGiftClaim(Player player, boolean awaiting) {
        awaitingGiftClaim.put(player.getUniqueId(), awaiting);
    }

    /**
     * 移除玩家的等待状态
     * @param player 玩家对象
     */
    public void removeAwaitingGiftClaim(Player player) {
        awaitingGiftClaim.remove(player.getUniqueId());
    }
}
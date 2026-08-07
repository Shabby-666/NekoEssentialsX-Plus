package com.nekoessentialsx.integration;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import com.nekoessentialsx.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * NekoEssentialX ↔ NextNeko 头衔集成
 * 将 NextNeko 配置中的猫娘聊天前缀注册为一个可佩戴/卸下的头衔，
 * 猫娘玩家加入服务器/打开主菜单时会自动获得该头衔（不强制佩戴）。
 */
public class NekoTitleIntegration implements Listener {

    public static final String NEKO_TITLE_ID = "nextneko";
    private static final String NEKO_TITLE_NAME = "猫娘";
    private static final String NEKO_TITLE_PERMISSION = "nextneko.title.auto";

    private final NekoEssentialX plugin;
    private final NextNekoBridge bridge;

    public NekoTitleIntegration(NekoEssentialX plugin, NextNekoBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    /**
     * 同步 NextNeko 聊天前缀为头衔，并为在线猫娘发放
     */
    public void sync() {
        if (!bridge.isInstalled()) {
            return;
        }
        String prefix = bridge.getConfig().getString("neko-chat.prefix", "§6[猫娘] §f");
        TitleManager titleManager = plugin.getTitleManager();
        if (titleManager == null) {
            return;
        }
        try {
            if (titleManager.getTitle(NEKO_TITLE_ID) == null) {
                titleManager.createTitle(NEKO_TITLE_ID, NEKO_TITLE_NAME, prefix, "", NEKO_TITLE_PERMISSION, 0, true);
            } else {
                titleManager.editTitle(NEKO_TITLE_ID, NEKO_TITLE_NAME, prefix, "", NEKO_TITLE_PERMISSION, 0, true);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("同步 NextNeko 猫娘头衔失败: " + e.getMessage());
        }

        // 为在线的猫娘补发头衔
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bridge.isNeko(player)) {
                grantNekoTitle(player);
            }
        }
    }

    /**
     * 为猫娘玩家发放（非强制佩戴）头衔
     */
    public void grantNekoTitle(Player player) {
        if (player == null || !bridge.isInstalled()) {
            return;
        }
        TitleManager titleManager = plugin.getTitleManager();
        DatabaseManager db = plugin.getDatabaseManager();
        if (titleManager == null || db == null) {
            return;
        }
        String playerId = player.getName();
        if (!db.hasTitle(playerId, NEKO_TITLE_ID)) {
            db.addTitleToInventory(playerId, NEKO_TITLE_ID, false);
        }
    }

    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        sync();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!bridge.isInstalled()) {
            return;
        }
        Player player = event.getPlayer();
        if (bridge.isNeko(player)) {
            grantNekoTitle(player);
        }
    }
}
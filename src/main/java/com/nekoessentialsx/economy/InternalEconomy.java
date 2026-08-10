package com.nekoessentialsx.economy;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.entity.Player;

/**
 * 插件内置经济后端（纯数据库实现，不依赖 Vault）。
 *
 * <p>当服务器未安装 Vault 时，所有经济操作直接走此实现；
 * 已安装 Vault 时由 {@link VaultBridge} 优先接入外部经济服务。</p>
 */
public class InternalEconomy {
    private final NekoEssentialX plugin;

    public InternalEconomy(NekoEssentialX plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        return plugin.getDatabaseManager().getPlayerBalance(player.getName());
    }

    /**
     * 给玩家充值
     */
    public boolean deposit(Player player, double amount) {
        return plugin.getDatabaseManager().addPlayerBalance(player.getName(), amount);
    }

    /**
     * 从玩家账户扣款
     */
    public boolean withdraw(Player player, double amount) {
        return plugin.getDatabaseManager().subtractPlayerBalance(player.getName(), amount);
    }

    /**
     * 检查玩家是否有足够余额
     */
    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }
}
package com.nekoessentialsx.economy;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 经济管理器。
 *
 * <p>本类不直接引用任何 Vault API 类（否则服务器未安装 Vault 时，
 * 类加载即抛 {@link NoClassDefFoundError}）。所有 Vault 相关逻辑都
 * 隔离在 {@link VaultBridge} 中，仅在检测到 Vault 已加载时才被访问。</p>
 */
public class EconomyManager {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private InternalEconomy internalEconomy;
    private VaultBridge vaultBridge;

    public EconomyManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = DatabaseManager.getInstance(plugin);
    }

    /**
     * 初始化经济管理器
     */
    public boolean initialize() {
        try {
            // 内置经济后端（纯数据库，永不依赖 Vault）
            internalEconomy = new InternalEconomy(plugin);

            // 仅当 Vault 存在且启用时，才尝试接入（防止加载 Vault 类失败）
            org.bukkit.plugin.Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
            if (vaultPlugin != null && vaultPlugin.isEnabled()) {
                plugin.getLogger().info("检测到Vault插件 (v" + vaultPlugin.getDescription().getVersion() + ")，尝试接入经济服务。");
                try {
                    vaultBridge = VaultBridge.trySetup(this);
                    plugin.getLogger().info(vaultBridge.isUsingInternal()
                            ? "已启用内置经济服务：NekoEssentialsX+Economy（已注册到Vault）"
                            : "已启用Vault外部经济系统：" + vaultBridge.getEconomyName());
                } catch (Throwable t) {
                    vaultBridge = null;
                    plugin.getLogger().warning("接入 Vault 失败，改用内置经济服务：" + t.getMessage());
                }
            } else {
                plugin.getLogger().info("Vault插件未加载，将使用内置经济服务。");
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("初始化经济服务时出错：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEnabled() {
        return internalEconomy != null;
    }

    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        if (!isEnabled()) {
            return 0;
        }
        return vaultBridge != null ? vaultBridge.getBalance(player) : internalEconomy.getBalance(player);
    }

    /**
     * 给玩家充值
     */
    public boolean depositPlayer(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return vaultBridge != null
                ? vaultBridge.depositPlayer(player, amount)
                : internalEconomy.deposit(player, amount);
    }

    /**
     * 从玩家账户扣款
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return vaultBridge != null
                ? vaultBridge.withdrawPlayer(player, amount)
                : internalEconomy.withdraw(player, amount);
    }

    /**
     * 玩家之间转账
     */
    public boolean transfer(Player sender, Player recipient, double amount) {
        if (!isEnabled()) {
            return false;
        }

        if (!withdrawPlayer(sender, amount)) {
            return false;
        }

        if (!depositPlayer(recipient, amount)) {
            depositPlayer(sender, amount);
            return false;
        }

        return true;
    }

    /**
     * 检查玩家是否有足够的余额
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return vaultBridge != null ? vaultBridge.has(player, amount) : internalEconomy.has(player, amount);
    }

    /**
     * 获取货币名称
     */
    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "金币");
    }

    /**
     * 获取货币名称（复数）
     */
    public String getCurrencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-name-plural", "金币");
    }

    /**
     * 格式化金额显示
     */
    public String format(double amount) {
        return (long) amount + " " + getCurrencyName();
    }

    /**
     * 是否正在使用内置经济服务
     */
    public boolean isUsingInternalEconomy() {
        return vaultBridge == null || vaultBridge.isUsingInternal();
    }

    /**
     * 获取内置经济后端（不依赖 Vault）
     */
    public InternalEconomy getInternalEconomy() {
        return internalEconomy;
    }

    /**
     * 重新加载货币名称
     */
    public void reloadCurrencyName() {
        // getCurrencyName 和 getCurrencyNamePlural 每次直接读配置，无需额外处理
    }

    /**
     * 获取插件实例
     */
    public NekoEssentialX getPlugin() {
        return plugin;
    }
}
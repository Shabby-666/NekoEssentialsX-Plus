package com.nekoessentialsx.economy;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final NekoEssentialX plugin;
    private Economy economy = null;
    private boolean vaultEnabled = false;
    private CatEconomyProvider internalEconomy = null;
    private boolean usingInternalEconomy = false;
    private final DatabaseManager databaseManager;

    public EconomyManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = DatabaseManager.getInstance(plugin);
    }

    /**
     * 初始化经济管理器
     */
    public boolean initialize() {
        try {
            // 检查Vault插件是否已加载
            org.bukkit.plugin.Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
            boolean vaultAvailable = vaultPlugin != null && vaultPlugin.isEnabled();

            if (vaultAvailable) {
                plugin.getLogger().info("检测到Vault插件 (v" + vaultPlugin.getDescription().getVersion() + ")，优先尝试使用外部经济系统。");

                // 获取Vault上已注册的经济服务（其它插件提供的）。
                // 注意：要先在注册我们自己的内置经济之前查询，否则拿到的是我们自己的服务。
                RegisteredServiceProvider<Economy> provider =
                        Bukkit.getServicesManager().getRegistration(Economy.class);

                // 只有当外部已有非内置的经济服务时才采用
                if (provider != null && provider.getProvider() != null
                        && !(provider.getProvider() instanceof CatEconomyProvider)) {
                    economy = provider.getProvider();
                    vaultEnabled = true;
                    usingInternalEconomy = false;
                    plugin.getLogger().info("优先使用Vault的外部经济系统：" + economy.getName());
                    return true;
                }

                plugin.getLogger().info("未找到外部Vault经济服务，回退使用内置经济系统。");
            } else {
                plugin.getLogger().info("Vault插件未加载，将使用内置经济服务。");
            }

            return enableInternalEconomy();
        } catch (Exception e) {
            plugin.getLogger().warning("初始化经济服务时出错：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启用内置经济服务
     */
    private boolean enableInternalEconomy() {
        try {
            // 创建内置经济服务提供商
            internalEconomy = new CatEconomyProvider(this);
            
            // 检查Vault是否可用，如果可用则注册内置经济服务
            org.bukkit.plugin.Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
            if (vaultPlugin != null) {
                // 注册内置经济服务到Vault
                Bukkit.getServicesManager().register(Economy.class, internalEconomy, plugin, org.bukkit.plugin.ServicePriority.High);
                plugin.getLogger().info("已将内置经济服务注册到Vault！");
                vaultEnabled = true;
            } else {
                plugin.getLogger().info("Vault不可用，将直接使用内置经济服务！");
                vaultEnabled = false;
            }
            
            economy = internalEconomy;
            usingInternalEconomy = true;
            plugin.getLogger().info("已启用内置经济服务：NekoEssentialsX+Economy");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("启用内置经济服务时出错：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEnabled() {
        return economy != null;
    }

    /**
     * 获取玩家余额
     * @param player 玩家
     * @return 玩家余额
     */
    public double getBalance(Player player) {
        if (!isEnabled()) {
            return 0;
        }
        return economy.getBalance(player);
    }

    /**
     * 给玩家充值
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean depositPlayer(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 从玩家账户扣款
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean withdrawPlayer(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 玩家之间转账
     * @param sender 转账玩家
     * @param recipient 收款玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean transfer(Player sender, Player recipient, double amount) {
        if (!isEnabled()) {
            return false;
        }

        // 先从发送者账户扣款
        if (!withdrawPlayer(sender, amount)) {
            return false;
        }

        // 再给接收者账户充值
        if (!depositPlayer(recipient, amount)) {
            // 如果充值失败，退款给发送者
            depositPlayer(sender, amount);
            return false;
        }

        return true;
    }

    /**
     * 检查玩家是否有足够的余额
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够的余额
     */
    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        return economy.has(player, amount);
    }

    /**
     * 获取货币名称
     * @return 货币名称
     */
    public String getCurrencyName() {
        return plugin.getConfig().getString("economy.currency-name", "金币");
    }
    
    /**
     * 获取货币名称（复数）
     * @return 货币名称（复数）
     */
    public String getCurrencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-name-plural", "金币");
    }
    
    /**
     * 格式化金额显示
     * @param amount 金额
     * @return 格式化后的金额
     */
    public String format(double amount) {
        return (int) amount + " " + getCurrencyName();
    }

    /**
     * 获取经济服务实例
     * @return 经济服务实例
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * 是否正在使用内置经济服务
     * @return 是否使用内置经济服务
     */
    public boolean isUsingInternalEconomy() {
        return usingInternalEconomy;
    }

    /**
     * 获取内置经济服务实例
     * @return 内置经济服务实例
     */
    public CatEconomyProvider getInternalEconomy() {
        return internalEconomy;
    }
    
    /**
     * 重新加载货币名称
     */
    public void reloadCurrencyName() {
        // 由于getCurrencyName和getCurrencyNamePlural方法每次都会直接从配置文件中读取，
        // 所以这个方法实际上不需要做任何事情，
        // 但为了保持代码一致性和可读性，我们仍然保留它
    }
    
    /**
     * 获取插件实例
     * @return 插件实例
     */
    public NekoEssentialX getPlugin() {
        return plugin;
    }
}
package com.nekoessentialsx.economy;

import com.nekoessentialsx.NekoEssentialX;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 接入桥。
 *
 * <p>此类集中了所有对 Vault API 的直接引用（Economy、EconomyResponse、服务注册等）。
 * 只允许在检测到 Vault 已加载时被调用，服务器未安装 Vault 时此类的类加载不会发生，
 * 从而避免 {@link NoClassDefFoundError}。</p>
 */
public class VaultBridge {
    private final NekoEssentialX plugin;
    private final Economy economy;
    private final boolean usingInternal;

    private VaultBridge(NekoEssentialX plugin, Economy economy, boolean usingInternal) {
        this.plugin = plugin;
        this.economy = economy;
        this.usingInternal = usingInternal;
    }

    /**
     * 尝试接入 Vault：优先采用外部已注册的经济服务，否则注册内置服务。
     * 仅当服务器已加载 Vault 插件时才允许调用。
     */
    public static VaultBridge trySetup(EconomyManager manager) {
        NekoEssentialX plugin = manager.getPlugin();

        // 先查询外部已注册的经济服务（避免拿到自己注册的）
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider != null && provider.getProvider() != null
                && !(provider.getProvider() instanceof CatEconomyProvider)) {
            plugin.getLogger().info("优先使用Vault的外部经济系统：" + provider.getProvider().getName());
            return new VaultBridge(plugin, provider.getProvider(), false);
        }

        // 没有外部服务，注册内置经济到 Vault
        CatEconomyProvider own = new CatEconomyProvider(manager);
        Bukkit.getServicesManager().register(Economy.class, own, plugin, org.bukkit.plugin.ServicePriority.High);
        plugin.getLogger().info("已将内置经济服务注册到Vault！");
        return new VaultBridge(plugin, own, true);
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public boolean depositPlayer(Player player, double amount) {
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean withdrawPlayer(Player player, double amount) {
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    /**
     * 当前是否使用内置经济（还是外部经济服务）
     */
    public boolean isUsingInternal() {
        return usingInternal;
    }

    /**
     * 当前经济服务的名称
     */
    public String getEconomyName() {
        return economy.getName();
    }
}
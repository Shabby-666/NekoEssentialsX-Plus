package com.nekoessentialsx.antiexplosion;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.antiexplosion.gui.ExplosionGUI;
import com.nekoessentialsx.antiexplosion.listener.ExplosionProtectionListener;
import com.nekoessentialsx.antiexplosion.manager.ExplosionProtectionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * 防爆系统模块。
 *
 * <p>原为独立插件 AntiExplosion，现作为 NekoEssentialX 的内建模块合入。
 * 负责加载防爆配置、注册事件监听器、注册 /explosion 与 /antiexplosion 命令。</p>
 */
public class AntiExplosionModule {
    private final NekoEssentialX plugin;
    private ExplosionProtectionManager explosionProtectionManager;
    private ExplosionGUI explosionGUI;

    public AntiExplosionModule(NekoEssentialX plugin) {
        this.plugin = plugin;
    }

    /**
     * 启用防爆模块：初始化管理器和 GUI，注册事件监听器。
     */
    public void onEnable() {
        // 保存默认配置（若文件不存在）
        if (!new File(plugin.getDataFolder(), "antiexplosion.yml").exists()) {
            plugin.saveResource("antiexplosion.yml", false);
        }

        // 初始化防爆管理器
        explosionProtectionManager = new ExplosionProtectionManager(this);

        // 初始化GUI系统
        explosionGUI = new ExplosionGUI(this);

        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(
                new ExplosionProtectionListener(this, explosionProtectionManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(explosionGUI, plugin);

        plugin.getLogger().info("防爆系统（AntiExplosion 模块）已成功加载！喵~");
    }

    /**
     * 卸载防爆模块。
     */
    public void onDisable() {
        if (explosionProtectionManager != null) {
            explosionProtectionManager.saveConfig();
        }
        plugin.getLogger().info("防爆系统（AntiExplosion 模块）已卸载！喵~");
    }

    /**
     * 热重载防爆配置。
     */
    public void reload() {
        if (explosionProtectionManager != null) {
            explosionProtectionManager.reloadConfig();
        }
    }

    /**
     * 处理 /explosion 与 /antiexplosion 命令。
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("explosion")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c呜...只有玩家才可以用这个命令的说~喵~");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("nekoessentialsx.antiexplosion.gui")) {
                player.sendMessage("§c呜...主人没有权限打开防爆配置界面啦的说~喵~");
                return true;
            }

            explosionGUI.openExplosionMenu(player);
            return true;
        }

        if (cmdName.equals("antiexplosion")) {
            if (args.length == 0) {
                sender.sendMessage("§6===== AntiExplosion 防爆系统 =====");
                sender.sendMessage("§a/antiexplosion reload §7- 重载配置文件");
                sender.sendMessage("§a/antiexplosion status §7- 查看当前状态");
                sender.sendMessage("§a/antiexplosion help §7- 显示帮助信息");
                sender.sendMessage("§a/explosion §7- 打开配置GUI（需要权限）");
                sender.sendMessage("§6================================");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    if (!sender.hasPermission("nekoessentialsx.antiexplosion.reload")) {
                        sender.sendMessage("§c呜...主人没有权限重载配置的说~喵~");
                        return true;
                    }
                    explosionProtectionManager.reloadConfig();
                    sender.sendMessage("§a呜呼~防爆配置已重载好啦的说~喵~");
                    return true;

                case "status":
                    sender.sendMessage("§6===== AntiExplosion 状态 =====");
                    sender.sendMessage("§7防爆系统状态: " + (explosionProtectionManager.isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7已单独配置的维度: " + (explosionProtectionManager.getConfiguredWorlds().isEmpty()
                            ? "§7无（全部使用默认配置）" : "§a" + String.join("§7, §a", explosionProtectionManager.getConfiguredWorlds())));
                    sender.sendMessage("§7默认维度-苦力怕: " + (explosionProtectionManager.getSource("default", "creeper").isEnabled() ? "§a启用" : "§c禁用")
                            + " | TNT: " + (explosionProtectionManager.getSource("default", "tnt").isEnabled() ? "§a启用" : "§c禁用")
                            + " | 末影水晶: " + (explosionProtectionManager.getSource("default", "end-crystal").isEnabled() ? "§a启用" : "§c禁用")
                            + " | 床: " + (explosionProtectionManager.getSource("default", "bed").isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7默认维度-实体破坏方块防护: " + (explosionProtectionManager.getBlockBreakConfig("default").isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7使用 §a/explosion §7进入 GUI 可按维度单独配置");
                    sender.sendMessage("§6==============================");
                    return true;

                case "help":
                    sender.sendMessage("§6===== AntiExplosion 帮助 =====");
                    sender.sendMessage("§a/explosion §7- 打开防爆系统配置GUI");
                    sender.sendMessage("§a/antiexplosion reload §7- 重载配置文件");
                    sender.sendMessage("§a/antiexplosion status §7- 查看当前状态");
                    sender.sendMessage("§a/antiexplosion help §7- 显示帮助信息");
                    sender.sendMessage("§6==============================");
                    return true;

                default:
                    sender.sendMessage("§c呜...这条命令我不认识的说，用 /antiexplosion help 看看有哪些吧喵~");
                    return true;
            }
        }

        return false;
    }

    public NekoEssentialX getPlugin() {
        return plugin;
    }

    public ExplosionProtectionManager getExplosionProtectionManager() {
        return explosionProtectionManager;
    }

    public ExplosionGUI getExplosionGUI() {
        return explosionGUI;
    }
}
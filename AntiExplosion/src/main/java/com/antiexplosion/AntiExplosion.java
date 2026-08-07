package com.antiexplosion;

import com.antiexplosion.gui.ExplosionGUI;
import com.antiexplosion.manager.ExplosionProtectionManager;
import com.antiexplosion.listener.ExplosionProtectionListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiExplosion extends JavaPlugin {
    private static AntiExplosion instance;
    private ExplosionProtectionManager explosionProtectionManager;
    private ExplosionGUI explosionGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化防爆管理器
        explosionProtectionManager = new ExplosionProtectionManager(this);
        
        // 初始化GUI系统
        explosionGUI = new ExplosionGUI(this);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(
                new ExplosionProtectionListener(this, explosionProtectionManager), this);
        getServer().getPluginManager().registerEvents(explosionGUI, this);
        
        getLogger().info("AntiExplosion防爆系统已成功加载！");
    }

    @Override
    public void onDisable() {
        // 保存配置
        if (explosionProtectionManager != null) {
            explosionProtectionManager.saveConfig();
        }
        
        getLogger().info("AntiExplosion防爆系统已卸载！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();
        
        if (cmdName.equals("explosion")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家可以使用此命令！");
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("antiexplosion.gui")) {
                player.sendMessage("§c你没有权限打开防爆系统配置界面！");
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
                    if (!sender.hasPermission("antiexplosion.reload")) {
                        sender.sendMessage("§c你没有权限重载配置！");
                        return true;
                    }
                    reloadConfig();
                    explosionProtectionManager.reloadConfig();
                    sender.sendMessage("§aAntiExplosion配置已重载！");
                    return true;
                    
                case "status":
                    sender.sendMessage("§6===== AntiExplosion 状态 =====");
                    sender.sendMessage("§7防爆系统状态: " + (explosionProtectionManager.isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7生物爆炸防护: " + (explosionProtectionManager.getEntityExplosionConfig().isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7TNT爆炸防护: " + (explosionProtectionManager.getTntExplosionConfig().isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7末影水晶爆炸防护: " + (explosionProtectionManager.getEndCrystalExplosionConfig().isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7床爆炸防护: " + (explosionProtectionManager.getBedExplosionConfig().isEnabled() ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§7实体破坏方块防护: " + (explosionProtectionManager.getEntityBlockBreakConfig().isEnabled() ? "§a启用" : "§c禁用"));
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
                    sender.sendMessage("§c未知命令，使用 /antiexplosion help 查看帮助");
                    return true;
            }
        }
        
        return false;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (explosionProtectionManager != null) {
            explosionProtectionManager.reloadConfig();
        }
    }

    public static AntiExplosion getInstance() {
        return instance;
    }

    public ExplosionProtectionManager getExplosionProtectionManager() {
        return explosionProtectionManager;
    }

    public ExplosionGUI getExplosionGUI() {
        return explosionGUI;
    }
}

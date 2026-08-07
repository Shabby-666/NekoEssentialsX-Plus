package com.antiexplosion.gui;

import com.antiexplosion.AntiExplosion;
import com.antiexplosion.manager.ExplosionProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExplosionGUI implements Listener {
    private final AntiExplosion plugin;
    private final Map<UUID, String> playerGUIs = new HashMap<>();
    
    private static final String GUI_MAIN_MENU = "explosion_main";
    private static final String GUI_CONFIG_MENU = "explosion_config";
    private static final String GUI_BLOCK_BREAK_MENU = "block_break_config";
    private static final String GUI_LOGGING_MENU = "logging_config";

    public ExplosionGUI(AntiExplosion plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开防爆系统主菜单
     * @param player 玩家
     */
    public void openExplosionMenu(Player player) {
        if (!player.hasPermission("antiexplosion.gui")) {
            player.sendMessage("§c你没有权限管理防爆系统！");
            return;
        }

        ExplosionProtectionManager manager = plugin.getExplosionProtectionManager();
        boolean enabled = manager.isEnabled();

        Inventory gui = Bukkit.createInventory(null, 54, "§c§l防爆系统 §7- 配置管理");

        // 填充背景
        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 总开关
        Material toggleMaterial = enabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        String toggleName = enabled ? "§a§l防爆系统已启用" : "§c§l防爆系统已禁用";
        gui.setItem(10, createItem(toggleMaterial, toggleName, 
                "§7当前状态: " + (enabled ? "§a启用" : "§c禁用"), "§7点击切换状态"));

        // 生物爆炸配置
        ExplosionProtectionManager.ExplosionConfig entityConfig = manager.getEntityExplosionConfig();
        gui.setItem(12, createExplosionConfigItem(Material.CREEPER_HEAD, "生物爆炸", entityConfig));

        // 实体破坏方块配置
        ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = manager.getEntityBlockBreakConfig();
        gui.setItem(14, createBlockBreakConfigItem(Material.DIAMOND_PICKAXE, "实体破坏方块", blockBreakConfig));

        // TNT爆炸配置
        ExplosionProtectionManager.ExplosionConfig tntConfig = manager.getTntExplosionConfig();
        gui.setItem(16, createExplosionConfigItem(Material.TNT, "TNT爆炸", tntConfig));

        // 末影水晶爆炸配置
        ExplosionProtectionManager.ExplosionConfig crystalConfig = manager.getEndCrystalExplosionConfig();
        gui.setItem(20, createExplosionConfigItem(Material.END_CRYSTAL, "末影水晶爆炸", crystalConfig));

        // 床爆炸配置
        ExplosionProtectionManager.ExplosionConfig bedConfig = manager.getBedExplosionConfig();
        gui.setItem(22, createExplosionConfigItem(Material.RED_BED, "床爆炸", bedConfig));

        // 其他爆炸配置
        ExplosionProtectionManager.ExplosionConfig otherConfig = manager.getOtherExplosionConfig();
        gui.setItem(24, createExplosionConfigItem(Material.FIRE_CHARGE, "其他爆炸", otherConfig));

        // 日志配置
        ExplosionProtectionManager.LoggingConfig loggingConfig = manager.getLoggingConfig();
        gui.setItem(30, createLoggingConfigItem(Material.BOOK, "日志配置", loggingConfig));

        // 保存配置按钮
        gui.setItem(40, createItem(Material.WRITABLE_BOOK, "§a§l保存配置", "§7将所有配置保存到文件"));

        // 重载配置按钮
        gui.setItem(42, createItem(Material.CLOCK, "§e§l重载配置", "§7从文件重新加载配置"));

        // 关闭按钮
        gui.setItem(49, createItem(Material.BARRIER, "§c§l关闭菜单", "§7点击关闭此菜单"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_MAIN_MENU);
    }

    /**
     * 打开爆炸配置菜单
     * @param player 玩家
     * @param configType 配置类型
     * @param displayName 显示名称
     */
    public void openExplosionConfigMenu(Player player, String configType, String displayName) {
        if (!player.hasPermission("antiexplosion.config")) {
            player.sendMessage("§c你没有权限！");
            return;
        }

        ExplosionProtectionManager manager = plugin.getExplosionProtectionManager();
        ExplosionProtectionManager.ExplosionConfig config = getExplosionConfigByType(manager, configType);

        Inventory gui = Bukkit.createInventory(null, 27, "§c§l" + displayName + " §7- 配置");

        // 填充背景
        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 启用/禁用
        Material enableMaterial = config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        String enableName = config.isEnabled() ? "§a§l已启用" : "§c§l已禁用";
        gui.setItem(10, createItem(enableMaterial, enableName, "§7点击切换"));

        // 破坏方块
        Material breakMaterial = config.isBreakBlocks() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String breakName = config.isBreakBlocks() ? "§a§l允许破坏方块" : "§c§l禁止破坏方块";
        gui.setItem(12, createItem(breakMaterial, breakName, "§7当前: " + (config.isBreakBlocks() ? "§a允许" : "§c禁止")));

        // 伤害实体
        Material damageMaterial = config.isDamageEntities() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String damageName = config.isDamageEntities() ? "§a§l允许伤害实体" : "§c§l禁止伤害实体";
        gui.setItem(14, createItem(damageMaterial, damageName, "§7当前: " + (config.isDamageEntities() ? "§a允许" : "§c禁止")));

        // 返回按钮
        gui.setItem(18, createItem(Material.ARROW, "§e§l返回", "§7返回主菜单"));

        // 关闭按钮
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭", "§7关闭菜单"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_CONFIG_MENU + ":" + configType + ":" + displayName);
    }

    /**
     * 打开实体破坏方块配置菜单
     * @param player 玩家
     */
    public void openBlockBreakConfigMenu(Player player) {
        if (!player.hasPermission("antiexplosion.config")) {
            player.sendMessage("§c你没有权限！");
            return;
        }

        ExplosionProtectionManager manager = plugin.getExplosionProtectionManager();
        ExplosionProtectionManager.EntityBlockBreakConfig config = manager.getEntityBlockBreakConfig();

        Inventory gui = Bukkit.createInventory(null, 27, "§c§l实体破坏方块 §7- 配置");

        // 填充背景
        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 启用/禁用
        Material enableMaterial = config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        String enableName = config.isEnabled() ? "§a§l已启用" : "§c§l已禁用";
        gui.setItem(10, createItem(enableMaterial, enableName, "§7点击切换"));

        // 允许破坏
        Material breakMaterial = config.isAllowBreak() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String breakName = config.isAllowBreak() ? "§a§l允许破坏方块" : "§c§l禁止破坏方块";
        gui.setItem(12, createItem(breakMaterial, breakName, "§7当前: " + (config.isAllowBreak() ? "§a允许" : "§c禁止")));

        // 应用到所有实体
        Material allMaterial = config.isApplyToAllEntities() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String allName = config.isApplyToAllEntities() ? "§a§l应用到所有实体" : "§c§l仅应用到指定实体";
        gui.setItem(14, createItem(allMaterial, allName, "§7当前: " + (config.isApplyToAllEntities() ? "§a是" : "§c否")));

        // 返回按钮
        gui.setItem(18, createItem(Material.ARROW, "§e§l返回", "§7返回主菜单"));

        // 关闭按钮
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭", "§7关闭菜单"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_BLOCK_BREAK_MENU);
    }

    /**
     * 打开日志配置菜单
     * @param player 玩家
     */
    public void openLoggingConfigMenu(Player player) {
        if (!player.hasPermission("antiexplosion.config")) {
            player.sendMessage("§c你没有权限！");
            return;
        }

        ExplosionProtectionManager manager = plugin.getExplosionProtectionManager();
        ExplosionProtectionManager.LoggingConfig config = manager.getLoggingConfig();

        Inventory gui = Bukkit.createInventory(null, 27, "§c§l日志配置 §7- 设置");

        // 填充背景
        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 启用/禁用
        Material enableMaterial = config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        String enableName = config.isEnabled() ? "§a§l日志已启用" : "§c§l日志已禁用";
        gui.setItem(10, createItem(enableMaterial, enableName, "§7点击切换"));

        // 详细日志
        Material detailMaterial = config.isDetailed() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String detailName = config.isDetailed() ? "§a§l详细日志" : "§c§l简单日志";
        gui.setItem(12, createItem(detailMaterial, detailName, "§7当前: " + (config.isDetailed() ? "§a详细" : "§c简单")));

        // 记录拦截
        Material blockedMaterial = config.isLogBlocked() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String blockedName = config.isLogBlocked() ? "§a§l记录拦截事件" : "§c§l不记录拦截事件";
        gui.setItem(14, createItem(blockedMaterial, blockedName, "§7当前: " + (config.isLogBlocked() ? "§a是" : "§c否")));

        // 记录允许
        Material allowedMaterial = config.isLogAllowed() ? Material.GREEN_WOOL : Material.RED_WOOL;
        String allowedName = config.isLogAllowed() ? "§a§l记录允许事件" : "§c§l不记录允许事件";
        gui.setItem(16, createItem(allowedMaterial, allowedName, "§7当前: " + (config.isLogAllowed() ? "§a是" : "§c否")));

        // 返回按钮
        gui.setItem(18, createItem(Material.ARROW, "§e§l返回", "§7返回主菜单"));

        // 关闭按钮
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭", "§7关闭菜单"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_LOGGING_MENU);
    }

    /**
     * 创建爆炸配置项
     */
    private ItemStack createExplosionConfigItem(Material material, String name, ExplosionProtectionManager.ExplosionConfig config) {
        String color = config.isEnabled() ? "§a" : "§c";
        return createItem(material, color + "§l" + name,
                "§7状态: " + (config.isEnabled() ? "§a启用" : "§c禁用"),
                "§7破坏方块: " + (config.isBreakBlocks() ? "§a允许" : "§c禁止"),
                "§7伤害实体: " + (config.isDamageEntities() ? "§a允许" : "§c禁止"),
                "§e点击配置");
    }

    /**
     * 创建实体破坏方块配置项
     */
    private ItemStack createBlockBreakConfigItem(Material material, String name, ExplosionProtectionManager.EntityBlockBreakConfig config) {
        String color = config.isEnabled() ? "§a" : "§c";
        return createItem(material, color + "§l" + name,
                "§7状态: " + (config.isEnabled() ? "§a启用" : "§c禁用"),
                "§7允许破坏: " + (config.isAllowBreak() ? "§a允许" : "§c禁止"),
                "§7应用到所有实体: " + (config.isApplyToAllEntities() ? "§a是" : "§c否"),
                "§e点击配置");
    }

    /**
     * 创建日志配置项
     */
    private ItemStack createLoggingConfigItem(Material material, String name, ExplosionProtectionManager.LoggingConfig config) {
        String color = config.isEnabled() ? "§a" : "§c";
        return createItem(material, color + "§l" + name,
                "§7状态: " + (config.isEnabled() ? "§a启用" : "§c禁用"),
                "§7详细日志: " + (config.isDetailed() ? "§a是" : "§c否"),
                "§7记录拦截: " + (config.isLogBlocked() ? "§a是" : "§c否"),
                "§7记录允许: " + (config.isLogAllowed() ? "§a是" : "§c否"),
                "§e点击配置");
    }

    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 填充空位
     */
    private void fillEmpty(Inventory gui, Material material) {
        ItemStack emptyItem = createItem(material, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, emptyItem);
            }
        }
    }

    /**
     * 根据类型获取爆炸配置
     */
    private ExplosionProtectionManager.ExplosionConfig getExplosionConfigByType(ExplosionProtectionManager manager, String type) {
        return switch (type) {
            case "entity-explosion" -> manager.getEntityExplosionConfig();
            case "tnt-explosion" -> manager.getTntExplosionConfig();
            case "end-crystal-explosion" -> manager.getEndCrystalExplosionConfig();
            case "bed-explosion" -> manager.getBedExplosionConfig();
            case "other-explosion" -> manager.getOtherExplosionConfig();
            default -> manager.getOtherExplosionConfig();
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String guiType = playerGUIs.get(player.getUniqueId());
        
        if (guiType == null) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ExplosionProtectionManager manager = plugin.getExplosionProtectionManager();
        int slot = event.getSlot();

        if (guiType.equals(GUI_MAIN_MENU)) {
            handleMainMenuClick(player, slot, manager);
        } else if (guiType.startsWith(GUI_CONFIG_MENU)) {
            handleConfigMenuClick(player, slot, guiType, manager);
        } else if (guiType.equals(GUI_BLOCK_BREAK_MENU)) {
            handleBlockBreakMenuClick(player, slot, manager);
        } else if (guiType.equals(GUI_LOGGING_MENU)) {
            handleLoggingMenuClick(player, slot, manager);
        }
    }

    private void handleMainMenuClick(Player player, int slot, ExplosionProtectionManager manager) {
        switch (slot) {
            case 10 -> {
                // 总开关
                boolean newState = !manager.isEnabled();
                manager.setEnabled(newState);
                player.sendMessage(newState ? "§a防爆系统已启用！" : "§c防爆系统已禁用！");
                openExplosionMenu(player);
            }
            case 12 -> openExplosionConfigMenu(player, "entity-explosion", "生物爆炸");
            case 14 -> openBlockBreakConfigMenu(player);
            case 16 -> openExplosionConfigMenu(player, "tnt-explosion", "TNT爆炸");
            case 20 -> openExplosionConfigMenu(player, "end-crystal-explosion", "末影水晶爆炸");
            case 22 -> openExplosionConfigMenu(player, "bed-explosion", "床爆炸");
            case 24 -> openExplosionConfigMenu(player, "other-explosion", "其他爆炸");
            case 30 -> openLoggingConfigMenu(player);
            case 40 -> {
                manager.saveConfig();
                player.sendMessage("§a防爆系统配置已保存！");
            }
            case 42 -> {
                manager.reloadConfig();
                player.sendMessage("§a防爆系统配置已重载！");
                openExplosionMenu(player);
            }
            case 49 -> player.closeInventory();
        }
    }

    private void handleConfigMenuClick(Player player, int slot, String guiType, ExplosionProtectionManager manager) {
        String[] parts = guiType.split(":");
        String configType = parts[1];
        String displayName = parts[2];

        ExplosionProtectionManager.ExplosionConfig config = getExplosionConfigByType(manager, configType);

        switch (slot) {
            case 10 -> {
                // 启用/禁用
                toggleExplosionConfig(configType, !config.isEnabled(), config.isBreakBlocks(), config.isDamageEntities(), manager);
                openExplosionConfigMenu(player, configType, displayName);
            }
            case 12 -> {
                // 破坏方块
                toggleExplosionConfig(configType, config.isEnabled(), !config.isBreakBlocks(), config.isDamageEntities(), manager);
                openExplosionConfigMenu(player, configType, displayName);
            }
            case 14 -> {
                // 伤害实体
                toggleExplosionConfig(configType, config.isEnabled(), config.isBreakBlocks(), !config.isDamageEntities(), manager);
                openExplosionConfigMenu(player, configType, displayName);
            }
            case 18 -> openExplosionMenu(player);
            case 26 -> player.closeInventory();
        }
    }

    private void handleBlockBreakMenuClick(Player player, int slot, ExplosionProtectionManager manager) {
        ExplosionProtectionManager.EntityBlockBreakConfig config = manager.getEntityBlockBreakConfig();

        switch (slot) {
            case 10 -> {
                manager.setEntityBlockBreakConfig(!config.isEnabled(), config.isAllowBreak(), config.isApplyToAllEntities());
                openBlockBreakConfigMenu(player);
            }
            case 12 -> {
                manager.setEntityBlockBreakConfig(config.isEnabled(), !config.isAllowBreak(), config.isApplyToAllEntities());
                openBlockBreakConfigMenu(player);
            }
            case 14 -> {
                manager.setEntityBlockBreakConfig(config.isEnabled(), config.isAllowBreak(), !config.isApplyToAllEntities());
                openBlockBreakConfigMenu(player);
            }
            case 18 -> openExplosionMenu(player);
            case 26 -> player.closeInventory();
        }
    }

    private void handleLoggingMenuClick(Player player, int slot, ExplosionProtectionManager manager) {
        ExplosionProtectionManager.LoggingConfig config = manager.getLoggingConfig();

        switch (slot) {
            case 10 -> {
                manager.setLoggingConfig(!config.isEnabled(), config.isDetailed(), config.isLogBlocked(), config.isLogAllowed());
                openLoggingConfigMenu(player);
            }
            case 12 -> {
                manager.setLoggingConfig(config.isEnabled(), !config.isDetailed(), config.isLogBlocked(), config.isLogAllowed());
                openLoggingConfigMenu(player);
            }
            case 14 -> {
                manager.setLoggingConfig(config.isEnabled(), config.isDetailed(), !config.isLogBlocked(), config.isLogAllowed());
                openLoggingConfigMenu(player);
            }
            case 16 -> {
                manager.setLoggingConfig(config.isEnabled(), config.isDetailed(), config.isLogBlocked(), !config.isLogAllowed());
                openLoggingConfigMenu(player);
            }
            case 18 -> openExplosionMenu(player);
            case 26 -> player.closeInventory();
        }
    }

    private void toggleExplosionConfig(String type, boolean enabled, boolean breakBlocks, boolean damageEntities, ExplosionProtectionManager manager) {
        switch (type) {
            case "entity-explosion" -> manager.setEntityExplosionConfig(enabled, breakBlocks, damageEntities);
            case "tnt-explosion" -> manager.setTntExplosionConfig(enabled, breakBlocks, damageEntities);
            case "end-crystal-explosion" -> manager.setEndCrystalExplosionConfig(enabled, breakBlocks, damageEntities);
            case "bed-explosion" -> manager.setBedExplosionConfig(enabled, breakBlocks, damageEntities);
            case "other-explosion" -> manager.setOtherExplosionConfig(enabled, breakBlocks, damageEntities);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            playerGUIs.remove(event.getPlayer().getUniqueId());
        }
    }
}

package com.nekoessentialsx.antiexplosion.gui;

import com.nekoessentialsx.antiexplosion.AntiExplosionModule;
import com.nekoessentialsx.antiexplosion.manager.ExplosionProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 防爆系统GUI。
 *
 * <p>结构：维度列表（主菜单）→ 每个维度的独立配置页（11 种爆炸源）→
 * 每个爆炸源的详细设置页（破坏方块 / 伤害玩家 / 伤害生物等独立开关）。</p>
 */
public class ExplosionGUI implements Listener {
    private final AntiExplosionModule module;
    private final Map<UUID, String> playerGUIs = new HashMap<>();

    private static final String GUI_MAIN = "main:";
    private static final String GUI_WORLD = "world:";
    private static final String GUI_SOURCE = "source:";
    private static final String GUI_BLOCK_BREAK = "blockbreak:";
    private static final String GUI_LOGGING = "logging";

    private static final int WORLD_SLOTS_PER_PAGE = 15;
    private static final int WORLD_AREA_START = 19;

    /** 每个爆炸源的展示信息：名称、材质 */
    private record SourceInfo(String key, String name, Material material) {}

    private static final List<SourceInfo> SOURCES = List.of(
            new SourceInfo("creeper", "苦力怕", Material.CREEPER_HEAD),
            new SourceInfo("wither", "凋零", Material.WITHER_SKELETON_SKULL),
            new SourceInfo("ender-dragon", "末影龙", Material.DRAGON_HEAD),
            new SourceInfo("ghast-fireball", "恶魂火球", Material.FIRE_CHARGE),
            new SourceInfo("blaze-fireball", "烈焰人火球", Material.BLAZE_ROD),
            new SourceInfo("wind", "风弹", Material.FEATHER),
            new SourceInfo("tnt", "TNT", Material.TNT),
            new SourceInfo("end-crystal", "末影水晶", Material.END_CRYSTAL),
            new SourceInfo("bed", "床", Material.RED_BED),
            new SourceInfo("respawn-anchor", "重生锚", Material.RESPAWN_ANCHOR),
            new SourceInfo("other", "其他爆炸", Material.GUNPOWDER));

    private static final int[] SOURCE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};

    public ExplosionGUI(AntiExplosionModule module) {
        this.module = module;
    }

    /* ==================== 打开界面 ==================== */

    /**
     * 打开防爆系统主菜单（维度列表）
     */
    public void openExplosionMenu(Player player) {
        openExplosionMenu(player, 0);
    }

    public void openExplosionMenu(Player player, int page) {
        if (!player.hasPermission("nekoessentialsx.antiexplosion.gui")) {
            player.sendMessage("§c呜...主人没有权限管理防爆系统的说~喵~");
            return;
        }

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();

        Inventory gui = Bukkit.createInventory(null, 54, "§c§l防爆系统 §7- 维度配置");

        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 总开关
        boolean enabled = manager.isEnabled();
        gui.setItem(10, createItem(enabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                enabled ? "§a§l防爆系统已启用喵~" : "§c§l防爆系统已禁用喵~",
                "§7当前状态: " + (enabled ? "§a启用" : "§c禁用"), "§7点击切换状态"));

        // 默认维度配置入口
        ExplosionProtectionManager.ProfileConfig defaultProfile = manager.getDefaultProfile();
        gui.setItem(11, createItem(Material.GRASS_BLOCK, "§6§l默认维度 §7- 未单独配置的世界",
                "§7未单独配置的世界自动套用此页设置",
                "§7苦力怕: " + state(defaultProfile.getSource("creeper").isEnabled()),
                "§7TNT: " + state(defaultProfile.getSource("tnt").isEnabled()),
                "§7末影水晶: " + state(defaultProfile.getSource("end-crystal").isEnabled()),
                "§e点击配置"));

        // 日志配置
        gui.setItem(13, createItem(Material.BOOK, "§b§l日志配置喵~", "§7点击配置日志记录", "§e点击配置"));

        // 保存 / 重载
        gui.setItem(16, createItem(Material.WRITABLE_BOOK, "§a§l保存配置喵~", "§7把所有配置保存到文件里~"));
        gui.setItem(17, createItem(Material.CLOCK, "§e§l重载配置喵~", "§7从文件重新加载配置的说~"));

        // 维度列表（支持翻页）
        List<String> worldNames = getWorldNames();
        int start = page * WORLD_SLOTS_PER_PAGE;
        int end = Math.min(start + WORLD_SLOTS_PER_PAGE, worldNames.size());
        for (int i = start; i < end; i++) {
            String worldName = worldNames.get(i);
            gui.setItem(WORLD_AREA_START + (i - start), createWorldItem(worldName));
        }
        if (page > 0) {
            gui.setItem(WORLD_AREA_START - 1, createItem(Material.ARROW, "§e§l上一页喵~", "§7第 " + page + " 页"));
        }
        if (end < worldNames.size()) {
            gui.setItem(WORLD_AREA_START + WORLD_SLOTS_PER_PAGE,
                    createItem(Material.ARROW, "§e§l下一页喵~", "§7第 " + (page + 2) + " 页"));
        }

        // 关闭按钮
        gui.setItem(49, createItem(Material.BARRIER, "§c§l关闭菜单喵~", "§7点击关闭这个菜单的说~"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_MAIN + page);
    }

    /**
     * 打开某个维度的配置页
     */
    public void openWorldPage(Player player, String worldName) {
        if (!player.hasPermission("nekoessentialsx.antiexplosion.config")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        String displayName = worldName.equals("default") ? "默认维度" : worldName;

        Inventory gui = Bukkit.createInventory(null, 54,
                "§c§l" + displayName + " §7- 防爆设置");

        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < SOURCES.size(); i++) {
            SourceInfo info = SOURCES.get(i);
            ExplosionProtectionManager.SourceConfig config =
                    manager.getSource(worldName, info.key());
            gui.setItem(SOURCE_SLOTS[i], createSourceItem(info, config));
        }

        // 实体破坏方块
        ExplosionProtectionManager.EntityBlockBreakConfig blockBreak =
                manager.getBlockBreakConfig(worldName);
        gui.setItem(SOURCE_SLOTS[11] + 1, createItem(Material.DIAMOND_PICKAXE,
                (blockBreak.isEnabled() ? "§a§l" : "§c§l") + "实体破坏方块",
                "§7状态: " + state(blockBreak.isEnabled()),
                "§7允许破坏: " + state(blockBreak.isAllowBreak()),
                "§7应用到所有实体: " + state(blockBreak.isApplyToAllEntities()),
                "§e点击配置"));

        // 返回维度列表
        gui.setItem(18, createItem(Material.ARROW, "§e§l返回喵~", "§7返回维度列表的说~"));

        // 关闭
        gui.setItem(49, createItem(Material.BARRIER, "§c§l关闭喵~", "§7关闭菜单的说~"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_WORLD + worldName);
    }

    /**
     * 打开某个维度下某爆炸源的详细设置页
     */
    public void openSourcePage(Player player, String worldName, String sourceKey) {
        if (!player.hasPermission("nekoessentialsx.antiexplosion.config")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        SourceInfo info = SOURCES.stream().filter(s -> s.key().equals(sourceKey)).findFirst().orElse(null);
        if (info == null) {
            openWorldPage(player, worldName);
            return;
        }

        ExplosionProtectionManager.SourceConfig config = manager.getSource(worldName, sourceKey);
        String dimensionName = worldName.equals("default") ? "默认维度" : worldName;

        Inventory gui = Bukkit.createInventory(null, 27,
                "§c§l" + info.name() + " §7- " + dimensionName);

        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        // 启用/禁用
        gui.setItem(10, createItem(config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                config.isEnabled() ? "§a§l防护已启用喵~" : "§c§l防护已禁用喵~",
                "§7当前: " + state(config.isEnabled()), "§7点击切换"));

        // 允许破坏方块
        gui.setItem(12, createItem(config.isBreakBlocks() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isBreakBlocks() ? "§a§l允许破坏方块喵~" : "§c§l禁止破坏方块喵~",
                "§7当前: " + state(config.isBreakBlocks()),
                "§7关闭后只保护地形，爆炸伤害不受影响",
                "§7点击切换"));

        // 允许伤害玩家
        gui.setItem(14, createItem(config.isDamagePlayers() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isDamagePlayers() ? "§a§l允许伤害玩家喵~" : "§c§l保护玩家喵~",
                "§7当前: " + state(config.isDamagePlayers()),
                "§7关闭后玩家不再受到此类爆炸伤害，",
                "§7方块破坏与生物伤害不受影响",
                "§7点击切换"));

        // 允许伤害生物
        gui.setItem(16, createItem(config.isDamageEntities() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isDamageEntities() ? "§a§l允许伤害生物喵~" : "§c§l保护生物喵~",
                "§7当前: " + state(config.isDamageEntities()),
                "§7关闭后其他生物不再受到此类爆炸伤害",
                "§7点击切换"));

        // 威力倍率调整
        gui.setItem(20, createItem(Material.BRICK, "§e§l威力 -0.5",
                "§7当前威力: §f" + formatPower(config.getPowerMultiplier())));
        gui.setItem(21, createItem(Material.EMERALD, "§e§l威力 +0.5",
                "§7当前威力: §f" + formatPower(config.getPowerMultiplier())));

        // 最大半径调整
        gui.setItem(22, createItem(Material.STICK, "§e§l最大半径 -10",
                "§7当前半径: §f" + formatRadius(config.getMaxRadius())));
        gui.setItem(23, createItem(Material.ENDER_PEARL, "§e§l最大半径 +10",
                "§7当前半径: §f" + formatRadius(config.getMaxRadius())));

        // 返回
        gui.setItem(18, createItem(Material.ARROW, "§e§l返回喵~", "§7返回维度配置的说~"));

        // 关闭
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭喵~", "§7关闭菜单的说~"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_SOURCE + worldName + ":" + sourceKey);
    }

    /**
     * 打开实体破坏方块配置页
     */
    public void openBlockBreakPage(Player player, String worldName) {
        if (!player.hasPermission("nekoessentialsx.antiexplosion.config")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        ExplosionProtectionManager.EntityBlockBreakConfig config =
                manager.getBlockBreakConfig(worldName);
        String dimensionName = worldName.equals("default") ? "默认维度" : worldName;

        Inventory gui = Bukkit.createInventory(null, 27,
                "§c§l实体破坏方块 §7- " + dimensionName);

        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        gui.setItem(10, createItem(config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                config.isEnabled() ? "§a§l已启用喵~" : "§c§l已禁用喵~",
                "§7当前: " + state(config.isEnabled()), "§7点击切换"));

        gui.setItem(12, createItem(config.isAllowBreak() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isAllowBreak() ? "§a§l允许破坏方块喵~" : "§c§l禁止破坏方块喵~",
                "§7当前: " + state(config.isAllowBreak()), "§7点击切换"));

        gui.setItem(14, createItem(config.isApplyToAllEntities() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isApplyToAllEntities() ? "§a§l应用到所有实体喵~" : "§c§l仅应用到指定实体喵~",
                "§7当前: " + state(config.isApplyToAllEntities()), "§7点击切换"));

        gui.setItem(18, createItem(Material.ARROW, "§e§l返回喵~", "§7返回维度配置的说~"));
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭喵~", "§7关闭菜单的说~"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_BLOCK_BREAK + worldName);
    }

    /**
     * 打开日志配置菜单
     */
    public void openLoggingConfigMenu(Player player) {
        if (!player.hasPermission("nekoessentialsx.antiexplosion.config")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        ExplosionProtectionManager.LoggingConfig config = manager.getLoggingConfig();

        Inventory gui = Bukkit.createInventory(null, 27, "§c§l日志配置 §7- 设置");

        fillEmpty(gui, Material.GRAY_STAINED_GLASS_PANE);

        gui.setItem(10, createItem(config.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                config.isEnabled() ? "§a§l日志已启用喵~" : "§c§l日志已禁用喵~",
                "§7点击切换"));

        gui.setItem(12, createItem(config.isDetailed() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isDetailed() ? "§a§l详细日志喵~" : "§c§l简单日志喵~",
                "§7当前: " + state(config.isDetailed())));

        gui.setItem(14, createItem(config.isLogBlocked() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isLogBlocked() ? "§a§l记录拦截事件喵~" : "§c§l不记录拦截事件喵~",
                "§7当前: " + state(config.isLogBlocked())));

        gui.setItem(16, createItem(config.isLogAllowed() ? Material.GREEN_WOOL : Material.RED_WOOL,
                config.isLogAllowed() ? "§a§l记录允许事件喵~" : "§c§l不记录允许事件喵~",
                "§7当前: " + state(config.isLogAllowed())));

        gui.setItem(18, createItem(Material.ARROW, "§e§l返回喵~", "§7返回主菜单的说~"));
        gui.setItem(26, createItem(Material.BARRIER, "§c§l关闭喵~", "§7关闭菜单的说~"));

        player.openInventory(gui);
        playerGUIs.put(player.getUniqueId(), GUI_LOGGING);
    }

    /* ==================== 点击处理 ==================== */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String guiType = playerGUIs.get(player.getUniqueId());
        if (guiType == null) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        if (guiType.startsWith(GUI_MAIN)) {
            handleMainClick(player, slot, guiType);
        } else if (guiType.startsWith(GUI_WORLD)) {
            handleWorldClick(player, slot, guiType.substring(GUI_WORLD.length()));
        } else if (guiType.startsWith(GUI_SOURCE)) {
            handleSourceClick(player, slot, guiType.substring(GUI_SOURCE.length()));
        } else if (guiType.startsWith(GUI_BLOCK_BREAK)) {
            handleBlockBreakClick(player, slot, guiType.substring(GUI_BLOCK_BREAK.length()));
        } else if (guiType.equals(GUI_LOGGING)) {
            handleLoggingClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot, String guiType) {
        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        int page = parsePage(guiType.substring(GUI_MAIN.length()));

        switch (slot) {
            case 10 -> {
                boolean newState = !manager.isEnabled();
                manager.setEnabled(newState);
                player.sendMessage(newState
                        ? "§a呜呼~防爆系统已经打开啦的说~喵~"
                        : "§c呜...防爆系统被关掉了的说，小心被炸到喵~");
                openExplosionMenu(player, page);
            }
            case 11 -> openWorldPage(player, "default");
            case 13 -> openLoggingConfigMenu(player);
            case 16 -> {
                manager.saveConfig();
                player.sendMessage("§a呜呼~防爆系统配置保存好了的说~喵~");
            }
            case 17 -> {
                manager.reloadConfig();
                player.sendMessage("§a呜呼~防爆系统配置重新加载好啦的说~喵~");
                openExplosionMenu(player, page);
            }
            case 18 -> {
                if (page > 0) {
                    openExplosionMenu(player, page - 1);
                }
            }
            case 34 -> {
                List<String> worlds = getWorldNames();
                if ((page + 1) * WORLD_SLOTS_PER_PAGE < worlds.size()) {
                    openExplosionMenu(player, page + 1);
                }
            }
            case 49 -> player.closeInventory();
            default -> {
                int index = slot - WORLD_AREA_START;
                if (index >= 0 && index < WORLD_SLOTS_PER_PAGE) {
                    int target = page * WORLD_SLOTS_PER_PAGE + index;
                    List<String> worlds = getWorldNames();
                    if (target < worlds.size()) {
                        openWorldPage(player, worlds.get(target));
                    }
                }
            }
        }
    }

    private void handleWorldClick(Player player, int slot, String worldName) {
        ExplosionProtectionManager manager = module.getExplosionProtectionManager();

        // 实体破坏方块入口（位于 SOURCE_SLOTS[11] + 1 = 25）
        if (slot == 25) {
            openBlockBreakPage(player, worldName);
            return;
        }

        if (slot == 18) {
            openExplosionMenu(player);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        int index = indexOfSlot(slot);
        if (index < 0) {
            return;
        }
        openSourcePage(player, worldName, SOURCES.get(index).key());
    }

    private void handleSourceClick(Player player, int slot, String context) {
        int sep = context.lastIndexOf(':');
        if (sep < 0) {
            openExplosionMenu(player);
            return;
        }
        String worldName = context.substring(0, sep);
        String sourceKey = context.substring(sep + 1);

        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        ExplosionProtectionManager.SourceConfig config = manager.getSource(worldName, sourceKey);

        switch (slot) {
            case 10 -> config.setEnabled(!config.isEnabled());
            case 12 -> config.setBreakBlocks(!config.isBreakBlocks());
            case 14 -> config.setDamagePlayers(!config.isDamagePlayers());
            case 16 -> config.setDamageEntities(!config.isDamageEntities());
            case 20 -> config.setPowerMultiplier(Math.max(0.1, round1(config.getPowerMultiplier() - 0.5)));
            case 21 -> config.setPowerMultiplier(Math.min(10.0, round1(config.getPowerMultiplier() + 0.5)));
            case 22 -> config.setMaxRadius(Math.max(0, config.getMaxRadius() - 10));
            case 23 -> config.setMaxRadius(Math.min(1000, config.getMaxRadius() + 10));
            case 18 -> {
                openWorldPage(player, worldName);
                return;
            }
            case 26 -> player.closeInventory();
            default -> {
                return;
            }
        }

        manager.setSourceConfig(worldName, sourceKey, config);
        openSourcePage(player, worldName, sourceKey);
    }

    private void handleBlockBreakClick(Player player, int slot, String worldName) {
        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        ExplosionProtectionManager.EntityBlockBreakConfig config =
                manager.getBlockBreakConfig(worldName);

        switch (slot) {
            case 10 -> manager.setBlockBreakConfig(worldName, !config.isEnabled(),
                    config.isAllowBreak(), config.isApplyToAllEntities());
            case 12 -> manager.setBlockBreakConfig(worldName, config.isEnabled(),
                    !config.isAllowBreak(), config.isApplyToAllEntities());
            case 14 -> manager.setBlockBreakConfig(worldName, config.isEnabled(),
                    config.isAllowBreak(), !config.isApplyToAllEntities());
            case 18 -> {
                openWorldPage(player, worldName);
                return;
            }
            case 26 -> player.closeInventory();
            default -> {
                return;
            }
        }
        openBlockBreakPage(player, worldName);
    }

    private void handleLoggingClick(Player player, int slot) {
        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        ExplosionProtectionManager.LoggingConfig config = manager.getLoggingConfig();

        switch (slot) {
            case 10 -> manager.setLoggingConfig(!config.isEnabled(), config.isDetailed(),
                    config.isLogBlocked(), config.isLogAllowed());
            case 12 -> manager.setLoggingConfig(config.isEnabled(), !config.isDetailed(),
                    config.isLogBlocked(), config.isLogAllowed());
            case 14 -> manager.setLoggingConfig(config.isEnabled(), config.isDetailed(),
                    !config.isLogBlocked(), config.isLogAllowed());
            case 16 -> manager.setLoggingConfig(config.isEnabled(), config.isDetailed(),
                    config.isLogBlocked(), !config.isLogAllowed());
            case 18 -> {
                openExplosionMenu(player);
                return;
            }
            case 26 -> player.closeInventory();
            default -> {
                return;
            }
        }
        openLoggingConfigMenu(player);
    }

    /* ==================== 辅助方法 ==================== */

    private List<String> getWorldNames() {
        List<String> names = new ArrayList<>();
        names.add("default");
        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }
        return names;
    }

    private ItemStack createWorldItem(String worldName) {
        ExplosionProtectionManager manager = module.getExplosionProtectionManager();
        boolean isDefault = worldName.equals("default");
        boolean configured = !isDefault && manager.getConfiguredWorlds().contains(worldName);

        Material material = Material.OAK_PLANKS;
        if (isDefault) {
            material = Material.GRASS_BLOCK;
        } else {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                material = switch (world.getEnvironment()) {
                    case NETHER -> Material.NETHERRACK;
                    case THE_END -> Material.END_STONE;
                    default -> Material.GRASS_BLOCK;
                };
            }
        }

        String name = (isDefault ? "§6§l默认维度" : "§b§l" + worldName);
        return createItem(material, name,
                "§7" + (configured ? "§a已单独配置" : "§7使用默认配置"),
                "§e点击进入该维度的防爆设置");
    }

    private ItemStack createSourceItem(SourceInfo info, ExplosionProtectionManager.SourceConfig config) {
        String color = config.isEnabled() ? "§a" : "§c";
        return createItem(info.material(), color + "§l" + info.name(),
                "§7状态: " + state(config.isEnabled()),
                "§7破坏方块: " + state(config.isBreakBlocks()),
                "§7伤害玩家: " + state(config.isDamagePlayers()),
                "§7伤害生物: " + state(config.isDamageEntities()),
                "§e点击配置");
    }

    private int indexOfSlot(int slot) {
        for (int i = 0; i < SOURCE_SLOTS.length; i++) {
            if (SOURCE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private int parsePage(String pageStr) {
        try {
            return Math.max(0, Integer.parseInt(pageStr));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String state(boolean value) {
        return value ? "§a允许/开启" : "§c禁止/关闭";
    }

    private String formatPower(double value) {
        return String.format("%.1f", value);
    }

    private String formatRadius(double value) {
        return value <= 0 ? "不限" : String.valueOf((int) value);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

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

    private void fillEmpty(Inventory gui, Material material) {
        ItemStack emptyItem = createItem(material, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, emptyItem);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            playerGUIs.remove(event.getPlayer().getUniqueId());
        }
    }
}

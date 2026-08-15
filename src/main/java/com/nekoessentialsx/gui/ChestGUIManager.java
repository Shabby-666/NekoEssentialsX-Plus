package com.nekoessentialsx.gui;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import com.nekoessentialsx.economy.EconomyManager;
import com.nekoessentialsx.integration.NekoTitleIntegration;
import com.nekoessentialsx.integration.NextNekoBridge;
import com.nekoessentialsx.kits.KitManager;
import com.nekoessentialsx.titles.TitleManager;
import com.nekoessentialsx.tpa.TPAManager;
import com.nekoessentialsx.warp.WarpManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * 箱子GUI管理器
 * 统一管理所有GUI界面，支持双调用方式（指令+GUI）
 */
public class ChestGUIManager {
    
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private final TitleManager titleManager;
    private final WarpManager warpManager;
    private final KitManager kitManager;
    private final TPAManager tpaManager;
    private final NextNekoBridge nextNekoBridge;
    
    // 存储玩家当前打开的GUI
    private final Map<UUID, ChestGUI> playerGUIs;
    // 存储玩家GUI历史记录（用于返回功能）
    private final Map<UUID, Stack<String>> playerGUIHistory;
    
    // GUI ID常量
    public static final String GUI_MAIN_MENU = "main_menu";
    public static final String GUI_HOME_MENU = "home_menu";
    public static final String GUI_WARP_MENU = "warp_menu";
    public static final String GUI_KIT_MENU = "kit_menu";
    public static final String GUI_TITLE_MENU = "title_menu";
    public static final String GUI_ECONOMY_MENU = "economy_menu";
    public static final String GUI_TP_MENU = "tp_menu";
    public static final String GUI_PLAYER_LIST = "player_list";
    public static final String GUI_CONFIRM = "confirm";
    
    // NextNeko 集成相关GUI ID
    public static final String GUI_NEKO_SKILLS = "neko_skills";
    public static final String GUI_NEKO_OWNER = "neko_owner";
    public static final String GUI_NEKO_OWNERS_LIST = "neko_owners_list";
    public static final String GUI_NEKO_NEKOS_LIST = "neko_nekos_list";
    public static final String GUI_NEKO_REQUESTS = "neko_requests";
    public static final String GUI_NEKO_ADMIN = "neko_admin";
    public static final String GUI_NEKO_MANAGE = "neko_manage";
    public static final String GUI_NEKO_NAME_SELECTOR = "neko_name_selector";
    public static final String GUI_NEXTNEKO_SETTINGS = "nextneko_settings";
    
    // 价格常量
    public static final int TITLE_COST = 100;
    public static final int CUSTOM_TITLE_COST = 1000;
    
    public ChestGUIManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        this.titleManager = plugin.getTitleManager();
        this.warpManager = plugin.getWarpManager();
        this.kitManager = plugin.getKitManager();
        this.tpaManager = plugin.getTPAManager();
        this.nextNekoBridge = plugin.getNextNekoBridge();
        this.playerGUIs = new HashMap<>();
        this.playerGUIHistory = new HashMap<>();
    }
    
    /**
     * 初始化GUI管理器
     */
    public void initialize() {
        // 注册GUI监听器
        plugin.getServer().getPluginManager().registerEvents(new ChestGUIListener(this), plugin);
    }
    
    /**
     * 获取插件实例
     * @return 插件实例
     */
    public NekoEssentialX getPlugin() {
        return plugin;
    }
    
    /**
     * 打开主菜单GUI
     * @param player 玩家
     */
    public void openMainMenu(Player player) {
        ChestGUI gui = new ChestGUI(player, "§6§lNekoEssentialsX+ §7- 主菜单", ChestGUI.DOUBLE_CHEST_SIZE, GUI_MAIN_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
// 家系统
        gui.setItem(10, ChestGUI.createCategoryIcon(Material.RED_BED, "家系统",
            "管理你的家，传送到已设置的家~", "§c", true),
            (p, click) -> openHomeMenu(p));
        
        // 传送点系统
        gui.setItem(12, ChestGUI.createCategoryIcon(Material.ENDER_PEARL, "传送点",
            "查看和传送到公共传送点~", "§9", true),
            (p, click) -> openWarpMenu(p, 1));
        
        // 工具包系统
        gui.setItem(14, ChestGUI.createCategoryIcon(Material.CHEST, "工具包",
            "领取各种工具包奖励的说~", "§6", true),
            (p, click) -> openKitMenu(p, 1));
        
        // 头衔系统
        gui.setItem(16, ChestGUI.createCategoryIcon(Material.NAME_TAG, "头衔系统",
            "购买、装备和管理你的头衔~喵~", "§d", true),
            (p, click) -> openTitleMenu(p));
        
        // 经济系统
        gui.setItem(28, ChestGUI.createCategoryIcon(Material.GOLD_INGOT, "经济系统",
            "查看余额、转账和管理经济~", "§e", true),
            (p, click) -> openEconomyMenu(p));
        
        // 传送系统
        gui.setItem(30, ChestGUI.createCategoryIcon(Material.COMPASS, "传送系统",
            "发送和接受传送请求~呜哼哼", "§b", true),
            (p, click) -> openTPMenu(p));
        
        // 玩家列表
        gui.setItem(32, ChestGUI.createCategoryIcon(Material.PLAYER_HEAD, "在线玩家",
            "看看现在都有谁在线的说~", "§a", true),
            (p, click) -> openPlayerList(p, 1));

        // 新手礼包
        if (plugin.getNewbieGiftManager().canClaimGift(player.getName())) {
            gui.setItem(34, ChestGUI.createCategoryIcon(Material.CHEST, "新手礼包",
                "来领取你的新手大礼包的说~喵~", "§5", true),
                (p, click) -> {
                    p.closeInventory();
                    plugin.getNewbieGiftManager().handleGiftClaim(p);
                });
        }
        
        // 每日签到
        gui.setItem(37, ChestGUI.createCategoryIcon(Material.CLOCK, "每日签到",
            "点击领取每日签到奖励的说~", "§3", true),
            (p, click) -> {
                p.closeInventory();
                plugin.getDailyLoginManager().handleDailyCheckIn(p);
            });
        
        // 插件信息
        gui.setItem(40, ChestGUI.createCategoryIcon(Material.BOOK, "插件信息",
            "查看插件版本和重载配置~", "§f", player.hasPermission("nekoessentialsx.admin")),
            (p, click) -> {
                if (p.hasPermission("nekoessentialsx.admin")) {
                    openPluginInfoMenu(p);
                } else {
                    p.sendMessage("§c呜...主人没有权限用这个功能的说~喵~");
                }
            });
        
        // AFK状态
        gui.setItem(43, ChestGUI.createCategoryIcon(Material.LEAD, "AFK状态",
            "切换你的AFK（离开）状态~", "§7", true),
            (p, click) -> {
                p.closeInventory();
                plugin.getAFKManager().setAFK(p, !plugin.getAFKManager().isAFK(p));
            });
        
        // ==================== NextNeko 集成入口 ====================
        if (plugin.isNextNekoInstalled() && nextNekoBridge != null) {
            boolean isNeko = nextNekoBridge.isNeko(player);
            
            // 猫娘专属技能管理（仅猫娘可见）
            if (isNeko) {
                gui.setItem(19, ChestGUI.createCategoryIcon(Material.CAT_SPAWN_EGG, "猫娘专属技能",
                    "管理只属于你的猫娘专属技能开关~", "§d", true),
                    (p, click) -> openNekoSkillMenu(p));
            } else {
                gui.setItem(19, ChestGUI.createCategoryIcon(Material.CAT_SPAWN_EGG, "猫娘专属技能",
                    "只有猫娘才能使用的技能管理~", "§8", false),
                    null);
            }

            // 主人与猫娘管理（猫娘或管理员）
            boolean canManageOwner = isNeko || player.hasPermission("nextneko.admin")
                    || player.hasPermission("nekoessentialsx.admin");
            gui.setItem(22, ChestGUI.createCategoryIcon(Material.NAME_TAG, "主人与猫娘管理",
                canManageOwner ? "查看和管理主人与猫娘的关系~" : "需要猫娘身份或管理员权限~", "§c", canManageOwner),
                canManageOwner ? (p, click) -> openOwnerMenu(p) : null);

            // NextNeko设置（管理员）
            boolean canSettings = player.hasPermission("nextneko.admin")
                    || player.hasPermission("nekoessentialsx.admin");
            gui.setItem(25, ChestGUI.createCategoryIcon(Material.COMMAND_BLOCK, "NextNeko设置",
                canSettings ? "在UI中编辑NextNeko的配置文件设置~" : "需要管理员权限~", "§e", canSettings),
                canSettings ? (p, click) -> openNextNekoSettingsMenu(p) : null);
            
            // 猫娘打开主菜单时补发【猫娘】头衔（对应右侧被占用 slot 42 冲突时）
            if (isNeko) {
                NekoTitleIntegration titleIntegration = plugin.getNekoTitleIntegration();
                if (titleIntegration != null) {
                    titleIntegration.grantNekoTitle(player);
                }
            }
        }
        
        // 关闭按钮
        gui.setItem(49, ChestGUI.createItem(Material.BARRIER, "§c§l关闭菜单喵~", 
            List.of("§7点击关闭这个菜单的说~"), "§c"),
            (p, click) -> p.closeInventory());
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_MAIN_MENU);
    }
    
    /**
     * 打开家菜单GUI
     * @param player 玩家
     */
    public void openHomeMenu(Player player) {
        ChestGUI gui = new ChestGUI(player, "§c§l家系统 §7- 管理你的家", ChestGUI.DOUBLE_CHEST_SIZE, GUI_HOME_MENU);
        
        List<String> homeNames = databaseManager != null ? databaseManager.getPlayerHomeNames(player.getName()) : null;
        if (homeNames == null) homeNames = new ArrayList<>();
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 显示家列表
        int slot = 0;
        for (String homeName : homeNames) {
            if (slot >= 45) break; // 保留底部导航栏
            
            Object[] homeInfo = databaseManager.getPlayerHome(player.getName(), homeName);
            if (homeInfo != null) {
                String worldName = (String) homeInfo[0];
                double x = (double) homeInfo[1];
                double y = (double) homeInfo[2];
                double z = (double) homeInfo[3];
                
                ItemStack homeItem = ChestGUI.createItem(Material.RED_BED, "§c§l" + homeName,
                    List.of(
                        "§7世界: §f" + worldName,
                        "§7坐标: §f" + String.format("%.1f, %.1f, %.1f", x, y, z),
                        "§a左键传送",
                        "§c右键删除"
                    ), "§c");
                
                final String finalHomeName = homeName;
                gui.setItem(slot, homeItem, (p, click) -> {
                    if (click == ChestGUI.ClickType.RIGHT) {
                        // 删除家
                        openConfirmGUI(p, "删除家", "§c确定要删除家 §l" + finalHomeName + " §c吗？",
                            (confirmPlayer) -> {
                                databaseManager.deletePlayerHome(confirmPlayer.getName(), finalHomeName);
                                confirmPlayer.sendMessage("§a呜呼~家 §l" + finalHomeName + " §a 被删掉啦喵~");
                                openHomeMenu(confirmPlayer);
                            },
                            (cancelPlayer) -> openHomeMenu(cancelPlayer));
                    } else {
                        // 传送
                        p.closeInventory();
                        teleportToHome(p, finalHomeName);
                    }
                });
                slot++;
            }
        }
        
        // 设置新家按钮
        if (slot < 45) {
            gui.setItem(slot, ChestGUI.createItem(Material.GREEN_CONCRETE, "§a§l设置新家",
                List.of("§7在当前位置设置一个新家", "§7左键点击设置"), "§a"),
                (p, click) -> {
                    p.closeInventory();
                    p.sendMessage("§a要设置新家的话，用指令 §e/sethome <家名称> §a就可以了哦，喵~");
                });
        }
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openMainMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_HOME_MENU);
    }
    
    /**
     * 打开传送点菜单GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openWarpMenu(Player player, int page) {
        if (warpManager == null) {
            player.sendMessage("§c呜...传送点管理器还没有加载好喵~");
            return;
        }
        List<String> warpNames = warpManager.getWarpNames();
        if (warpNames == null) warpNames = new ArrayList<>();
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) warpNames.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§9§l传送点 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_WARP_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, warpNames.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String warpName = warpNames.get(i);
            int slot = i - startIndex;
            
            ItemStack warpItem = ChestGUI.createItem(Material.ENDER_PEARL, "§9§l" + warpName,
                List.of("§7点击传送到此传送点"), "§9");
            
            gui.setItem(slot, warpItem, (p, click) -> {
                p.closeInventory();
                teleportToWarp(p, warpName);
            });
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int currentPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openMainMenu(p),
            (p, click) -> openWarpMenu(p, currentPage - 1),
            (p, click) -> openWarpMenu(p, currentPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_WARP_MENU);
    }
    
    /**
     * 打开工具包菜单GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openKitMenu(Player player, int page) {
        if (kitManager == null) {
            player.sendMessage("§c呜...工具包管理器还没有加载好喵~");
            return;
        }
        List<String> kitNames = kitManager.getKitNames();
        if (kitNames == null) kitNames = new ArrayList<>();
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) kitNames.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§6§l工具包 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_KIT_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, kitNames.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String kitName = kitNames.get(i);
            int slot = i - startIndex;
            
            String canClaim = kitManager.canClaimKit(player, kitName);
            boolean available = canClaim == null;
            
            Material material = available ? Material.CHEST : Material.BARRIER;
            String color = available ? "§6" : "§c";
            List<String> lore = new ArrayList<>();
            lore.add("§7点击领取此工具包");
            if (!available) {
                lore.add("§c" + canClaim);
            }
            
            ItemStack kitItem = ChestGUI.createItem(material, color + "§l" + kitName, lore, color);
            
            final String finalKitName = kitName;
            if (available) {
                gui.setItem(slot, kitItem, (p, click) -> {
                    p.closeInventory();
                    claimKit(p, finalKitName);
                });
            } else {
                gui.setItem(slot, kitItem, null);
            }
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int currentPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openMainMenu(p),
            (p, click) -> openKitMenu(p, currentPage - 1),
            (p, click) -> openKitMenu(p, currentPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_KIT_MENU);
    }
    
    /**
     * 打开头衔菜单GUI
     * @param player 玩家
     */
    public void openTitleMenu(Player player) {
        if (titleManager == null) {
            player.sendMessage("§c呜...头衔管理器还没有加载好喵~");
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§d§l头衔系统 §7- 选择功能", ChestGUI.SINGLE_CHEST_SIZE, GUI_TITLE_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        String currencyName = economyManager != null ? economyManager.getCurrencyName() : "金币";
        
        // 头衔商店
        gui.setItem(10, ChestGUI.createCategoryIcon(Material.CHEST, "头衔商店", 
            "购买新的头衔", "§a", true),
            (p, click) -> openTitleShop(p, 1));
        
        // 我的头衔
        gui.setItem(12, ChestGUI.createCategoryIcon(Material.BOOK, "我的头衔", 
            "查看和装备已拥有的头衔", "§b", true),
            (p, click) -> openMyTitles(p, 1));
        
        // 创建自定义头衔
        gui.setItem(14, ChestGUI.createCategoryIcon(Material.ANVIL, "创建自定义头衔", 
            "花费 " + CUSTOM_TITLE_COST + " " + currencyName + " 创建专属头衔", "§e", true),
            (p, click) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入你要创建的自定义头衔的说~喵~");
                p.sendMessage("§7格式：<id> <name>");
                p.sendMessage("§7示例：sakura 樱花喵");
                p.sendMessage("§7名称字符限制：1-20个字符的说~");
                p.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                plugin.getGuiManager().getChatInputListener().setPlayerInputState(p, ChatInputListener.InputState.WAITING_FOR_TITLE_NAME);
            });
        
        // 管理员管理
        if (player.hasPermission("nekoessentialsx.title.admin")) {
            gui.setItem(16, ChestGUI.createCategoryIcon(Material.COMMAND_BLOCK, "管理员管理", 
                "管理所有头衔", "§c", true),
                (p, click) -> openTitleAdminMenu(p, 1));
        }
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openMainMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_TITLE_MENU);
    }
    
    /**
     * 打开头衔商店GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openTitleShop(Player player, int page) {
        if (titleManager == null) {
            player.sendMessage("§c呜...头衔管理器还没有加载好喵~");
            return;
        }
        Map<String, TitleManager.Title> allTitles = titleManager.getTitles();
        if (allTitles == null) allTitles = new HashMap<>();
        List<String> availableTitles = new ArrayList<>();
        
        for (Map.Entry<String, TitleManager.Title> entry : allTitles.entrySet()) {
            if (entry.getValue().isEnabled() && 
                (player.hasPermission(entry.getValue().getPermission()) || 
                 player.hasPermission("nekoessentialsx.title.admin"))) {
                availableTitles.add(entry.getKey());
            }
        }
        
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) availableTitles.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§a§l头衔商店 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_TITLE_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableTitles.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = availableTitles.get(i);
            TitleManager.Title title = allTitles.get(titleId);
            int slot = i - startIndex;
            
            boolean owned = databaseManager.hasTitle(player.getName(), titleId);
            boolean equipped = titleId.equals(titleManager.getPlayerTitle(player.getName()));
            
            Material material = owned ? (equipped ? Material.DIAMOND_BLOCK : Material.DIAMOND) : Material.STONE;
            String color = owned ? (equipped ? "§a" : "§b") : "§7";
            
            List<String> lore = new ArrayList<>();
            lore.add("§7前缀: " + title.getPrefix());
            lore.add("§7ID: " + titleId);
            if (!owned) {
                lore.add("§6价格: " + TITLE_COST + " " + economyManager.getCurrencyName());
                lore.add("§a点击购买");
            } else if (equipped) {
                lore.add("§e当前已装备");
                lore.add("§c点击卸下");
            } else {
                lore.add("§a点击装备");
            }
            
            final String finalTitleId = titleId;
            final boolean finalOwned = owned;
            final boolean finalEquipped = equipped;
            final int currentPage = page;
            ItemStack titleItem = ChestGUI.createItem(material, 
                color + "§l" + title.getName() + (equipped ? " §a[已装备]" : ""), lore, color);
            
            gui.setItem(slot, titleItem, (p, click) -> {
                if (!finalOwned) {
                    // 购买头衔
                    buyTitle(p, finalTitleId);
                } else if (finalEquipped) {
                    // 卸下头衔
                    unequipTitle(p);
                } else {
                    // 装备头衔
                    equipTitle(p, finalTitleId);
                }
                openTitleShop(p, currentPage);
            });
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int navPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openTitleMenu(p),
            (p, click) -> openTitleShop(p, navPage - 1),
            (p, click) -> openTitleShop(p, navPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开我的头衔GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openMyTitles(Player player, int page) {
        List<String> ownedTitles = new ArrayList<>();
        
        // 添加系统头衔
        for (String titleId : titleManager.getTitles().keySet()) {
            if (databaseManager.hasTitle(player.getName(), titleId)) {
                ownedTitles.add(titleId);
            }
        }
        
        // 添加自定义头衔
        List<String> customTitleIds = databaseManager.getPlayerCustomTitles(player.getName());
        for (String customTitleId : customTitleIds) {
            if (databaseManager.hasTitle(player.getName(), customTitleId)) {
                ownedTitles.add(customTitleId);
            }
        }
        
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) ownedTitles.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§b§l我的头衔 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_TITLE_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, ownedTitles.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = ownedTitles.get(i);
            int slot = i - startIndex;
            
            TitleManager.Title systemTitle = titleManager.getTitle(titleId);
            Object[] customTitleData = databaseManager.getCustomTitle(titleId);
            
            String titleName;
            String prefix;
            boolean enabled = true;
            
            if (systemTitle != null) {
                titleName = systemTitle.getName();
                prefix = systemTitle.getPrefix();
                enabled = systemTitle.isEnabled();
            } else if (customTitleData != null) {
                titleName = (String) customTitleData[0];
                prefix = (String) customTitleData[1];
                enabled = (boolean) customTitleData[4];
            } else {
                continue;
            }
            
            if (!enabled) continue;
            
            boolean equipped = titleId.equals(titleManager.getPlayerTitle(player.getName()));
            Material material = equipped ? Material.DIAMOND_BLOCK : Material.DIAMOND;
            String color = equipped ? "§a" : "§b";
            
            List<String> lore = new ArrayList<>();
            lore.add("§7前缀: " + prefix);
            lore.add("§7ID: " + titleId);
            if (equipped) {
                lore.add("§e当前已装备");
                lore.add("§c点击卸下");
            } else {
                lore.add("§a点击装备");
            }
            
            final String finalTitleId = titleId;
            final boolean finalEquipped = equipped;
            ItemStack titleItem = ChestGUI.createItem(material, 
                color + "§l" + titleName + (equipped ? " §a[已装备]" : ""), lore, color);
            
            final int currentPage = page;
            gui.setItem(slot, titleItem, (p, click) -> {
                if (finalEquipped) {
                    unequipTitle(p);
                } else {
                    equipTitle(p, finalTitleId);
                }
                openMyTitles(p, currentPage);
            });
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int navPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openTitleMenu(p),
            (p, click) -> openMyTitles(p, navPage - 1),
            (p, click) -> openMyTitles(p, navPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开头衔管理员菜单
     * @param player 玩家
     * @param page 页码
     */
    public void openTitleAdminMenu(Player player, int page) {
        if (!player.hasPermission("nekoessentialsx.title.admin")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }
        
        Map<String, TitleManager.Title> allTitles = titleManager.getTitles();
        List<String> titleIds = new ArrayList<>(allTitles.keySet());
        
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) titleIds.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§c§l头衔管理 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_TITLE_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, titleIds.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = titleIds.get(i);
            TitleManager.Title title = allTitles.get(titleId);
            int slot = i - startIndex;
            
            Material material = title.isEnabled() ? Material.DIAMOND_BLOCK : Material.COAL_BLOCK;
            String color = title.isEnabled() ? "§a" : "§c";
            
            List<String> lore = new ArrayList<>();
            lore.add("§7前缀: " + title.getPrefix());
            lore.add("§7ID: " + titleId);
            lore.add("§7状态: " + (title.isEnabled() ? "§a启用" : "§c禁用"));
            lore.add("§a左键编辑");
            lore.add("§c右键删除");
            lore.add("§eShift+左键切换状态");
            
            final String finalTitleId = titleId;
            final boolean finalEnabled = title.isEnabled();
            final int currentPage = page;
            ItemStack titleItem = ChestGUI.createItem(material, 
                color + "§l" + title.getName(), lore, color);
            
            gui.setItem(slot, titleItem, (p, click) -> {
                if (click == ChestGUI.ClickType.RIGHT) {
                    // 删除
                    titleManager.deleteTitle(finalTitleId);
                    p.sendMessage("§a呜呼~头衔删掉啦喵~");
                } else if (click == ChestGUI.ClickType.SHIFT_LEFT) {
                    // 切换状态
                    titleManager.toggleTitleEnabled(finalTitleId, !finalEnabled);
                    p.sendMessage("§a呜呼~头衔状态切换好啦喵~");
                } else {
                    // 编辑
                    p.closeInventory();
                    p.sendMessage("§a编辑头衔要用指令来做的说~喵~");
                }
                openTitleAdminMenu(p, currentPage);
            });
        }
        
        // 添加创建按钮
        if (endIndex < 45) {
            gui.setItem(endIndex, ChestGUI.createItem(Material.GOLD_BLOCK, "§a§l创建新头衔",
                List.of("§7点击创建一个新的系统头衔"), "§a"),
                (p, click) -> {
                    p.closeInventory();
                    p.sendMessage("§a要创建新头衔的话，用指令 §e/playertitle admin create §a 的说~喵~");
                });
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int navPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openTitleMenu(p),
            (p, click) -> openTitleAdminMenu(p, navPage - 1),
            (p, click) -> openTitleAdminMenu(p, navPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开经济菜单GUI
     * @param player 玩家
     */
    public void openEconomyMenu(Player player) {
        if (economyManager == null) {
            player.sendMessage("§c呜...经济管理器还没有加载好喵~");
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§e§l经济系统 §7- 管理你的财富", ChestGUI.SINGLE_CHEST_SIZE, GUI_ECONOMY_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        double balance = economyManager.getBalance(player);
        
        // 查看余额
        gui.setItem(10, ChestGUI.createItem(Material.GOLD_NUGGET, "§e§l查看余额",
            List.of("§7当前余额: §6" + economyManager.format(balance), "§7点击刷新"), "§e"),
            (p, click) -> {
                double newBalance = economyManager.getBalance(p);
                p.sendMessage("§a主人的余额是: §6" + economyManager.format(newBalance) + " §a 的说~喵~");
                openEconomyMenu(p);
            });
        
        // 转账
        gui.setItem(12, ChestGUI.createItem(Material.GOLD_INGOT, "§6§l转账",
            List.of("§7转账给其他玩家", "§7点击选择玩家并输入金额"), "§6"),
            (p, click) -> openPlayerSelector(p, "转账", (target, amount) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入转账金额（输入 cancel 取消）：");
                plugin.getGuiManager().getChatInputListener().setPendingEconomyOperation(p, "transfer", target.getName());
            }));
        
        // 查看其他玩家余额
        gui.setItem(14, ChestGUI.createItem(Material.PAPER, "§b§l查看他人余额",
            List.of("§7查看其他玩家的余额"), "§b"),
            (p, click) -> openPlayerSelector(p, "查看余额", (target, amount) -> {
                double targetBalance = economyManager.getBalance(target);
                p.sendMessage("§a玩家 §e" + target.getName() + " §a 的余额是: §6" + economyManager.format(targetBalance) + " §a 的说~喵~");
            }));
        
        // 管理员功能
        if (player.hasPermission("nekoessentialsx.economy.admin")) {
            gui.setItem(16, ChestGUI.createCategoryIcon(Material.COMMAND_BLOCK, "管理员经济", 
                "管理其他玩家的经济", "§c", true),
                (p, click) -> openEconomyAdminMenu(p));
        }
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openMainMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_ECONOMY_MENU);
    }
    
    /**
     * 打开经济管理员菜单
     * @param player 玩家
     */
    public void openEconomyAdminMenu(Player player) {
        if (!player.hasPermission("nekoessentialsx.economy.admin")) {
            player.sendMessage("§c呜...主人没有权限的说~喵~");
            return;
        }
        if (economyManager == null) {
            player.sendMessage("§c呜...经济管理器还没有加载好喵~");
            return;
        }
        
        ChestGUI gui = new ChestGUI(player, "§c§l经济管理 §7- 管理员功能", ChestGUI.SINGLE_CHEST_SIZE, GUI_ECONOMY_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 充值
        gui.setItem(10, ChestGUI.createItem(Material.GREEN_CONCRETE, "§a§l充值",
            List.of("§7为玩家充值", "§7点击选择玩家并输入金额"), "§a"),
            (p, click) -> openPlayerSelector(p, "充值", (target, amount) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入充值金额（输入 cancel 取消）：");
                plugin.getGuiManager().getChatInputListener().setPendingEconomyOperation(p, "deposit", target.getName());
            }));
        
        // 扣款
        gui.setItem(12, ChestGUI.createItem(Material.RED_CONCRETE, "§c§l扣款",
            List.of("§7从玩家账户扣款", "§7点击选择玩家并输入金额"), "§c"),
            (p, click) -> openPlayerSelector(p, "扣款", (target, amount) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入扣款金额（输入 cancel 取消）：");
                plugin.getGuiManager().getChatInputListener().setPendingEconomyOperation(p, "withdraw", target.getName());
            }));
        
        // 设置余额
        gui.setItem(14, ChestGUI.createItem(Material.YELLOW_CONCRETE, "§e§l设置余额",
            List.of("§7直接设置玩家余额", "§7点击选择玩家并输入目标余额"), "§e"),
            (p, click) -> openPlayerSelector(p, "设置余额", (target, amount) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入目标余额（输入 cancel 取消）：");
                plugin.getGuiManager().getChatInputListener().setPendingEconomyOperation(p, "setbalance", target.getName());
            }));
        
        // 更改货币名称
        gui.setItem(16, ChestGUI.createItem(Material.NAME_TAG, "§d§l更改货币名称",
            List.of("§7更改游戏中的货币名称", "§7当前: §e" + economyManager.getCurrencyName()), "§d"),
            (p, click) -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天框中输入新的货币名称（输入 cancel 取消）：");
                plugin.getGuiManager().getChatInputListener().setPlayerInputState(p, ChatInputListener.InputState.WAITING_FOR_CURRENCY_NAME);
            });
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openEconomyMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开传送菜单GUI
     * @param player 玩家
     */
    public void openTPMenu(Player player) {
        if (tpaManager == null) {
            player.sendMessage("§c呜...传送管理器还没有加载好喵~");
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§b§l传送系统 §7- 管理传送请求", ChestGUI.SINGLE_CHEST_SIZE, GUI_TP_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 发送传送请求
        gui.setItem(10, ChestGUI.createItem(Material.PAPER, "§a§l发送传送请求",
            List.of("§7请求传送到其他玩家身边"), "§a"),
            (p, click) -> openPlayerSelector(p, "发送传送请求", (target, amount) -> {
                p.closeInventory();
                sendTPARequest(p, target);
            }));
        
        // 查看收到的请求
        gui.setItem(12, ChestGUI.createItem(Material.ENCHANTED_BOOK, "§b§l收到的请求",
            List.of("§7查看并处理收到的传送请求"), "§b"),
            (p, click) -> openTPRequestsMenu(p));
        
        // 取消已发送的请求
        gui.setItem(14, ChestGUI.createItem(Material.BARRIER, "§c§l取消请求",
            List.of("§7取消你发送的传送请求"), "§c"),
            (p, click) -> {
                p.closeInventory();
                p.sendMessage("§a要取消请求的话，用指令 §e/tpacancel [玩家] §a 的说~喵~");
            });
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openMainMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_TP_MENU);
    }
    
    /**
     * 打开传送请求菜单
     * @param player 玩家
     */
    public void openTPRequestsMenu(Player player) {
        ChestGUI gui = new ChestGUI(player, "§b§l传送请求 §7- 接受或拒绝", ChestGUI.DOUBLE_CHEST_SIZE, GUI_TP_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        List<com.nekoessentialsx.tpa.TPAManager.TPARequest> requests = tpaManager.getReceivedRequests(player);
        
        int slot = 0;
        for (com.nekoessentialsx.tpa.TPAManager.TPARequest request : requests) {
            if (slot >= 45) break;
            
            Player sender = request.getSender();
            ItemStack requestItem = ChestGUI.createItem(Material.PAPER, "§e§l" + sender.getName(),
                List.of("§7请求传送到你身边", "§a左键接受", "§c右键拒绝"), "§e");
            
            gui.setItem(slot, requestItem, (p, click) -> {
                if (click == ChestGUI.ClickType.RIGHT) {
                    denyTPARequest(p, sender.getName());
                } else {
                    acceptTPARequest(p, sender.getName());
                }
                openTPRequestsMenu(p);
            });
            slot++;
        }
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openTPMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开玩家选择器GUI
     * @param player 玩家
     * @param actionName 动作名称
     * @param callback 回调函数
     */
    public void openPlayerSelector(Player player, String actionName, PlayerSelectorCallback callback) {
        Collection<Player> onlinePlayers = (Collection<Player>) plugin.getServer().getOnlinePlayers();
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / itemsPerPage);
        
        openPlayerSelector(player, actionName, 1, callback);
    }
    
    /**
     * 打开玩家选择器GUI（带分页）
     * @param player 玩家
     * @param actionName 动作名称
     * @param page 页码
     * @param callback 回调函数
     */
    public void openPlayerSelector(Player player, String actionName, int page, PlayerSelectorCallback callback) {
        Collection<Player> onlinePlayers = new ArrayList<>((Collection<Player>) plugin.getServer().getOnlinePlayers());
        List<Player> playerList = new ArrayList<>(onlinePlayers);
        
        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) playerList.size() / itemsPerPage));
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§a§l选择玩家 §7- " + actionName + " (第 " + page + "/" + totalPages + " 页)", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_PLAYER_LIST);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Player target = playerList.get(i);
            int slot = i - startIndex;
            final Player finalTarget = target;
            
            ItemStack playerItem = ChestGUI.createItem(Material.PLAYER_HEAD, "§a§l" + target.getName(),
                List.of("§7点击选择此玩家"), "§a");
            
            gui.setItem(slot, playerItem, (p, click) -> callback.onSelect(finalTarget, 0));
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int currentPage = page;
        final String currentActionName = actionName;
        final PlayerSelectorCallback currentCallback = callback;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> goBack(p),
            (p, click) -> openPlayerSelector(p, currentActionName, currentPage - 1, currentCallback),
            (p, click) -> openPlayerSelector(p, currentActionName, currentPage + 1, currentCallback));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开确认GUI
     * @param player 玩家
     * @param title 标题
     * @param message 确认消息
     * @param onConfirm 确认回调
     * @param onCancel 取消回调
     */
    public void openConfirmGUI(Player player, String title, String message, 
                                ConfirmCallback onConfirm, ConfirmCallback onCancel) {
        ChestGUI gui = new ChestGUI(player, "§c§l确认: " + title, ChestGUI.SINGLE_CHEST_SIZE, GUI_CONFIRM);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 确认信息
        gui.setItem(13, ChestGUI.createItem(Material.PAPER, "§e§l" + message,
            List.of("§7请确认你的操作"), "§e"), null);
        
        // 确认按钮
        gui.setItem(11, ChestGUI.createItem(Material.GREEN_CONCRETE, "§a§l确认",
            List.of("§7点击确认操作"), "§a"),
            (p, click) -> onConfirm.onConfirm(p));
        
        // 取消按钮
        // 注意：传入的取消回调 lambda 实现的是 ConfirmCallback 的抽象方法 onConfirm，
        // onCancel 为默认空实现，若调用 onCancel() 将没有任何效果，故这里调用 onConfirm()
        gui.setItem(15, ChestGUI.createItem(Material.RED_CONCRETE, "§c§l取消",
            List.of("§7点击取消操作"), "§c"),
            (p, click) -> onCancel.onConfirm(p));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 打开自定义头衔创建确认GUI（新版箱子系统）
     * @param player 玩家
     * @param titleId 头衔ID
     * @param titleName 头衔名称
     */
    public void openConfirmCreateCustomTitleGUI(Player player, String titleId, String titleName) {
        // 再次确认ID未被占用，避免并发冲突
        if (titleManager.getTitle(titleId) != null || databaseManager.getCustomTitle(titleId) != null) {
            player.sendMessage("§c呜...头衔ID已经存在的说，请换一个ID重新创建吧喵~");
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> openTitleMenu(player));
            return;
        }

        String prefix = "[" + titleName + "] ";
        boolean isAdmin = player.hasPermission("nekoessentialsx.title.admin");
        int cost = isAdmin ? 0 : CUSTOM_TITLE_COST;
        String currencyName = economyManager != null ? economyManager.getCurrencyName() : "金币";

        openConfirmGUI(player, "创建自定义头衔",
            "创建头衔: " + titleId + " (" + titleName + ")" + (cost > 0 ? "  花费 " + cost + " " + currencyName : "（免费）"),
            p -> {
                // 确认创建
                if (!isAdmin) {
                    double balance = economyManager.getBalance(p);
                    if (balance < cost) {
                        p.sendMessage("§c呜...主人的余额不足啦！需要 " + cost + " " + currencyName + " 喵~");
                        openTitleMenu(p);
                        return;
                    }
                    if (!economyManager.withdrawPlayer(p, cost)) {
                        p.sendMessage("§c呜...创建失败了的说，等一小会儿再试试吧喵~");
                        openTitleMenu(p);
                        return;
                    }
                }

                // 保存自定义头衔
                if (databaseManager.saveCustomTitle(titleId, p.getName(), titleName, prefix, "")) {
                    // 添加到玩家仓库
                    databaseManager.addTitleToInventory(p.getName(), titleId, true);

                    // 发送成功消息
                    p.sendMessage("§a呜呼~自定义头衔创建成功啦喵~");
                    p.sendMessage("§a头衔ID: §b" + titleId);
                    p.sendMessage("§a头衔名称: §b" + titleName);

                    // 打开个人头衔界面（新版）
                    openMyTitles(p, 1);
                } else {
                    p.sendMessage("§c呜...创建失败了的说，等一小会儿再试试吧喵~");
                    openTitleMenu(p);
                }
            },
            p -> {
                p.sendMessage("§e呜...自定义头衔创建被取消啦，下次再来玩吧喵~");
                openTitleMenu(p);
            });
    }
    
    /**
     * 打开玩家列表GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openPlayerList(Player player, int page) {
        Collection<Player> onlinePlayers = new ArrayList<>((Collection<Player>) plugin.getServer().getOnlinePlayers());
        List<Player> playerList = new ArrayList<>(onlinePlayers);
        
        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) playerList.size() / itemsPerPage));
        page = Math.max(1, Math.min(page, totalPages));
        
        ChestGUI gui = new ChestGUI(player, "§a§l在线玩家 §7- 第 " + page + "/" + totalPages + " 页", 
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_PLAYER_LIST);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Player target = playerList.get(i);
            int slot = i - startIndex;
            final Player finalTarget = target;
            
            String afkStatus = plugin.getAFKManager().isAFK(target) ? " §7[AFK]" : "";
            ItemStack playerItem = ChestGUI.createItem(Material.PLAYER_HEAD, "§a§l" + target.getName() + afkStatus,
                List.of("§7点击查看详情", "§7世界: §f" + target.getWorld().getName()), "§a");
            
            gui.setItem(slot, playerItem, (p, click) -> {
                p.sendMessage("§a玩家: §e" + finalTarget.getName() + " §a 的说~喵~");
                p.sendMessage("§a世界: §e" + finalTarget.getWorld().getName() + " §a 的说~喵~");
                p.sendMessage("§a坐标: §e" + String.format("%.1f, %.1f, %.1f", 
                    finalTarget.getLocation().getX(), finalTarget.getLocation().getY(), finalTarget.getLocation().getZ()));
            });
        }
        
        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int currentPage = page;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> openMainMenu(p),
            (p, click) -> openPlayerList(p, currentPage - 1),
            (p, click) -> openPlayerList(p, currentPage + 1));
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_PLAYER_LIST);
    }
    
    /**
     * 打开插件信息菜单
     * @param player 玩家
     */
    public void openPluginInfoMenu(Player player) {
        ChestGUI gui = new ChestGUI(player, "§f§l插件信息", ChestGUI.SINGLE_CHEST_SIZE, GUI_MAIN_MENU);
        
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // 版本信息
        gui.setItem(10, ChestGUI.createItem(Material.BOOK, "§a§l查看版本",
            List.of("§7当前版本: §e1.4.1-beta"), "§a"),
            (p, click) -> {
                p.sendMessage("§a呜呼~NekoEssentialsX+ 现在的版本是: §e1.4.1-beta 喵~");
            });
        
        // 重载配置
        gui.setItem(12, ChestGUI.createItem(Material.COMMAND_BLOCK, "§6§l重载配置",
            List.of("§7重载插件配置文件"), "§6"),
            (p, click) -> {
                plugin.reloadConfig();
                p.sendMessage("§a呜呼~插件配置重载好啦的说~喵~");
            });
        
        // 添加导航栏
        gui.addNavigationBar(false, false, 
            (p, click) -> openMainMenu(p), null, null);
        
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }
    
    /**
     * 获取玩家的当前GUI
     * @param player 玩家
     * @return GUI对象，如果没有则返回null
     */
    public ChestGUI getPlayerGUI(Player player) {
        return playerGUIs.get(player.getUniqueId());
    }
    
    /**
     * 移除玩家的GUI
     * @param player 玩家
     */
    public void removePlayerGUI(Player player) {
        playerGUIs.remove(player.getUniqueId());
    }
    
    /**
     * 记录GUI历史
     * @param player 玩家
     * @param guiId GUI ID
     */
    private void recordGUIHistory(Player player, String guiId) {
        playerGUIHistory.computeIfAbsent(player.getUniqueId(), k -> new Stack<>()).push(guiId);
    }
    
    /**
     * 返回上一级菜单
     * @param player 玩家
     */
    public void goBack(Player player) {
        Stack<String> history = playerGUIHistory.get(player.getUniqueId());
        if (history != null && !history.isEmpty()) {
            history.pop(); // 移除当前
            if (!history.isEmpty()) {
                String previousGUI = history.peek();
                switch (previousGUI) {
                    case GUI_MAIN_MENU -> openMainMenu(player);
                    case GUI_HOME_MENU -> openHomeMenu(player);
                    case GUI_WARP_MENU -> openWarpMenu(player, 1);
                    case GUI_KIT_MENU -> openKitMenu(player, 1);
                    case GUI_TITLE_MENU -> openTitleMenu(player);
                    case GUI_ECONOMY_MENU -> openEconomyMenu(player);
                    case GUI_TP_MENU -> openTPMenu(player);
                    case GUI_PLAYER_LIST -> openPlayerList(player, 1);
                    case GUI_NEKO_SKILLS -> openNekoSkillMenu(player);
                    case GUI_NEKO_OWNER -> openOwnerMenu(player);
                    case GUI_NEKO_OWNERS_LIST -> openOwnersListMenu(player);
                    case GUI_NEKO_NEKOS_LIST -> openNekosListMenu(player);
                    case GUI_NEKO_REQUESTS -> openOwnerRequestsMenu(player);
                    case GUI_NEKO_ADMIN -> openNekoAdminMenu(player);
                    case GUI_NEKO_MANAGE -> openNekoAdminMenu(player);
                    case GUI_NEXTNEKO_SETTINGS -> openNextNekoSettingsMenu(player);
                    default -> openMainMenu(player);
                }
            } else {
                openMainMenu(player);
            }
        } else {
            openMainMenu(player);
        }
    }
    
    // ==================== 功能方法 ====================
    
    /**
     * 传送到家
     * @param player 玩家
     * @param homeName 家名称
     */
    public void teleportToHome(Player player, String homeName) {
        Object[] homeInfo = databaseManager.getPlayerHome(player.getName(), homeName);
        if (homeInfo == null) {
            player.sendMessage("§c呜...找不到这个家的说: " + homeName + " 喵~");
            return;
        }
        
        String worldName = (String) homeInfo[0];
        double x = (double) homeInfo[1];
        double y = (double) homeInfo[2];
        double z = (double) homeInfo[3];
        float yaw = (float) homeInfo[4];
        float pitch = (float) homeInfo[5];
        
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            player.sendMessage("§c呜...找不到世界的说: " + worldName + " 喵~");
            return;
        }
        
        Location homeLocation = new Location(world, x, y, z, yaw, pitch);
        player.teleport(homeLocation);
        player.sendMessage("§a呜呼~唰一下回到家啦: §e" + homeName + " 喵~");
    }
    
    /**
     * 传送到传送点
     * @param player 玩家
     * @param warpName 传送点名称
     */
    public void teleportToWarp(Player player, String warpName) {
        boolean success = warpManager.teleportToWarp(player, warpName);
        if (success) {
            player.sendMessage("§a呜呼~唰一下传送到传送点啦: §e" + warpName + " 喵~");
        } else {
            player.sendMessage("§c呜...传送失败了的说~喵~");
        }
    }
    
    /**
     * 领取工具包
     * @param player 玩家
     * @param kitName 工具包名称
     */
    public void claimKit(Player player, String kitName) {
        kitManager.giveKit(player, kitName);
    }
    
    /**
     * 购买头衔
     * @param player 玩家
     * @param titleId 头衔ID
     */
    public void buyTitle(Player player, String titleId) {
        TitleManager.Title title = titleManager.getTitle(titleId);
        if (title == null || !title.isEnabled()) {
            player.sendMessage("§c呜...这个头衔不存在，或者已经被关掉啦的说~喵~");
            return;
        }
        
        if (databaseManager.hasTitle(player.getName(), titleId)) {
            player.sendMessage("§e主人已经拥有这个头衔啦的说~喵~");
            return;
        }
        
        double balance = economyManager.getBalance(player);
        if (balance < TITLE_COST) {
            player.sendMessage("§c呜...主人的余额不够哦！需要 " + TITLE_COST + " " + economyManager.getCurrencyName() + " 的说~喵~");
            return;
        }
        
        if (economyManager.withdrawPlayer(player, TITLE_COST)) {
            databaseManager.addTitleToInventory(player.getName(), titleId, false);
            player.sendMessage("§a呜呼~买到了！主人获得头衔: §b" + title.getName() + " §a 的说~喵~");
        } else {
            player.sendMessage("§c呜...购买失败了的说~喵~");
        }
    }
    
    /**
     * 装备头衔
     * @param player 玩家
     * @param titleId 头衔ID
     */
    public void equipTitle(Player player, String titleId) {
        if (!databaseManager.hasTitle(player.getName(), titleId)) {
            player.sendMessage("§c呜...主人还没有这个头衔的说~喵~");
            return;
        }
        
        titleManager.updatePlayerTitle(player, titleId);
        
        TitleManager.Title title = titleManager.getTitle(titleId);
        Object[] customTitleData = databaseManager.getCustomTitle(titleId);
        String titleName = title != null ? title.getName() : (customTitleData != null ? (String) customTitleData[0] : titleId);
        
        player.sendMessage("§a呜呼~头衔戴好啦！现在的头衔是: §b" + titleName + " §a 的说~喵~");
    }
    
    /**
     * 卸下头衔
     * @param player 玩家
     */
    public void unequipTitle(Player player) {
        titleManager.clearPlayerTitle(player.getName());
        player.sendMessage("§a呜呼~头衔卸下啦，清清爽爽的喵~");
    }
    
    /**
     * 发送传送请求
     * @param player 发送者
     * @param target 目标玩家
     */
    public void sendTPARequest(Player player, Player target) {
        tpaManager.sendRequest(player, target);
    }
    
    /**
     * 接受传送请求
     * @param player 接受者
     * @param senderName 发送者名称
     */
    public void acceptTPARequest(Player player, String senderName) {
        tpaManager.acceptRequest(player, senderName);
    }
    
    /**
     * 拒绝传送请求
     * @param player 拒绝者
     * @param senderName 发送者名称
     */
    public void denyTPARequest(Player player, String senderName) {
        tpaManager.denyRequest(player, senderName);
    }

    // ==================== NextNeko 集成：猫娘专属技能管理 ====================

    /**
     * 打开猫娘专属技能管理GUI
     * 可在UI中开启/关闭 Neko 中只能用命令开关的配置
     */
    public void openNekoSkillMenu(Player player) {
        if (!plugin.isNextNekoInstalled() || !nextNekoBridge.isNeko(player)) {
            player.sendMessage("§c呜...只有猫娘才能使用这个功能的哦~喵~");
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§d§l猫娘专属技能 §7- 管理你的技能状态", ChestGUI.SINGLE_CHEST_SIZE, GUI_NEKO_SKILLS);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        // 接近提醒
        boolean notice = nextNekoBridge.isNoticeEnabled(player);
        gui.setItem(10, ChestGUI.createItem(Material.ENDER_EYE,
            (notice ? "§a§l接近提醒 [已开启]" : "§c§l接近提醒 [已关闭]"),
            List.of("§7主人和其它玩家靠近时是否提醒", "§7当前: " + (notice ? "§a已开启" : "§c已关闭"), "§a点击切换"), "§f"),
            (p, click) -> {
                nextNekoBridge.setNoticeEnabledDirect(p.getName(), !notice);
                p.sendMessage((!notice ? "§a呜呼~" : "§c") + "接近提醒已" + (!notice ? "开启" : "关闭") + "喵~");
                openNekoSkillMenu(p);
            });

        // 爬墙
        boolean climb = nextNekoBridge.isClimbEnabled(player);
        gui.setItem(12, ChestGUI.createItem(Material.LADDER,
            (climb ? "§a§l爬墙 [已开启]" : "§c§l爬墙 [已关闭]"),
            List.of("§7是否开启猫娘爬墙（贴墙漂浮）功能~", "§7当前: " + (climb ? "§a已开启" : "§c已关闭"), "§a点击切换"), "§6"),
            (p, click) -> {
                nextNekoBridge.setClimbEnabled(p, !climb);
                p.sendMessage((!climb ? "§a呜呼~爬墙功能开启" : "§c爬墙功能关闭") + "喵~");
                openNekoSkillMenu(p);
            });

        // 尾巴拉扯
        boolean tail = nextNekoBridge.isTailPullEnabled(player);
        gui.setItem(14, ChestGUI.createItem(Material.STRING,
            (tail ? "§a§l尾巴拉扯 [已开启]" : "§c§l尾巴拉扯 [已关闭]"),
            List.of("§7是否允许其它玩家薅你的尾巴~", "§7当前: " + (tail ? "§a已开启" : "§c已关闭"), "§a点击切换"), "§d"),
            (p, click) -> {
                nextNekoBridge.setTailPullEnabled(p, !tail);
                p.sendMessage((!tail ? "§a呜呼~尾巴拉扯已开启" : "§c尾巴拉扯已关闭") + "喵~");
                openNekoSkillMenu(p);
            });

        // 添加导航栏
        gui.addNavigationBar(false, false,
            (p, click) -> openMainMenu(p), null, null);

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        if (!playerGUIHistory.containsKey(player.getUniqueId()) || playerGUIHistory.get(player.getUniqueId()).isEmpty()
                || !GUI_NEKO_SKILLS.equals(playerGUIHistory.get(player.getUniqueId()).peek())) {
            recordGUIHistory(player, GUI_NEKO_SKILLS);
        }
    }

    // ==================== NextNeko 集成：主人与猫娘管理 ====================

    /**
     * 打开主人与猫娘管理菜单
     */
    public void openOwnerMenu(Player player) {
        if (!plugin.isNextNekoInstalled()) {
            player.sendMessage("§c呜...检测不到 NextNeko 插件，无法使用此功能~喵~");
            return;
        }
        boolean isNeko = nextNekoBridge.isNeko(player);
        boolean admin = player.hasPermission("nextneko.admin") || player.hasPermission("nekoessentialsx.admin");

        ChestGUI gui = new ChestGUI(player, "§c§l主人与猫娘管理", ChestGUI.SINGLE_CHEST_SIZE, GUI_NEKO_OWNER);
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        // 我的主人
        gui.setItem(10, ChestGUI.createItem(Material.HEART_OF_THE_SEA,
            "§a§l我的主人",
            isNeko ? List.of("§7查看我的主人列表并管理~") : List.of("§c你不是猫娘，没有主人~"),
            isNeko ? "§a" : "§8"),
            isNeko ? (p, click) -> openOwnersListMenu(p) : null);

        // 我的猫娘
        gui.setItem(12, ChestGUI.createCategoryIcon(Material.CAT_SPAWN_EGG, "我的猫娘",
            "§7查看我名下的猫娘并解除~", "§b", true),
            (p, click) -> openNekosListMenu(p));

        // 发送主人申请
        gui.setItem(14, ChestGUI.createCategoryIcon(Material.PAPER, "发送主人申请",
            "向猫娘发送申请，成为它的主人~", "§e", true),
            (p, click) -> openPlayerSelector(p, "选择要申请的主人/猫娘", (target, amount) -> {
                p.closeInventory();
                if (nextNekoBridge.hasOwnerRequest(p, target)) {
                    p.sendMessage("§c呜...已经发送过申请了喵~");
                    return;
                }
                if (nextNekoBridge.wouldCreateCycle(target.getName(), p.getName())) {
                    p.sendMessage("§c呜...这样会造成主人循环关系的喵~");
                    return;
                }
                nextNekoBridge.sendOwnerRequest(p, target);
                p.sendMessage("§a申请已发送给 §e" + target.getName() + " §a的说~喵~");
                if (nextNekoBridge.isNeko(target)) {
                    target.sendMessage("§e" + p.getName() + " §a想成为你的主人！使用 §e/owner accept " + p.getName() + " §a或 §e/owner deny " + p.getName() + " §a喵~");
                } else {
                    target.sendMessage("§e" + p.getName() + " §a想成为你的主人喵~（你是猫娘哦）");
                }
            }));

        // 收到的申请
        gui.setItem(16, ChestGUI.createCategoryIcon(Material.ENCHANTED_BOOK, "收到的申请",
            "查看并处理其它玩家发来的主人申请~", "§6", true),
            (p, click) -> openOwnerRequestsMenu(p));

        // 管理员管理全服主人关系
        if (admin) {
            gui.setItem(22, ChestGUI.createCategoryIcon(Material.COMMAND_BLOCK, "管理员：全服管理", 
                "强制建立/删除全服玩家的主人与猫娘关系", "§c", true),
                (p, click) -> openNekoAdminMenu(p));
        }
        // 添加导航栏
        gui.addNavigationBar(false, false,
            (p, click) -> openMainMenu(p), null, null);

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_OWNER);
    }

    /**
     * 我的主人列表
     */
    public void openOwnersListMenu(Player player) {
        Set<String> owners = nextNekoBridge.getOwnerNames(player.getName());
        ChestGUI gui = new ChestGUI(player, "§a§l我的主人", ChestGUI.DOUBLE_CHEST_SIZE, GUI_NEKO_OWNERS_LIST);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        if (owners.isEmpty()) {
            gui.setItem(22, ChestGUI.createItem(Material.BARRIER, "§c没有主人",
                List.of("§7还没有人成为你的主人~"), "§8"), null);
        } else {
            int slot = 0;
            for (String ownerName : owners) {
                if (slot >= 45) break;
                gui.setItem(slot, ChestGUI.createItem(Material.NAME_TAG, "主人角色",
                    List.of("主人: " + ownerName, "§c右键解除"), "§a"),
                    (p, click) -> openConfirmGUI(p, "解除主人", "§c确定要解除主人 §l" + ownerName + " §c的关系吗？",
                        (confirmPlayer) -> {
                            nextNekoBridge.removeOwnerDirect(confirmPlayer.getName(), ownerName);
                            confirmPlayer.sendMessage("§a呜呼~已解除与主人 §e" + ownerName + " §a的关系喵~");
                            openOwnersListMenu(confirmPlayer);
                        },
                        (cancelPlayer) -> openOwnersListMenu(cancelPlayer)));
                slot++;
            }
        }
        gui.addNavigationBar(false, false, (p, click) -> openOwnerMenu(p), null, null);
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_OWNERS_LIST);
    }

    /**
     * 我的猫娘列表
     */
    public void openNekosListMenu(Player player) {
        Set<String> nekos = nextNekoBridge.getNekoNamesByOwner(player.getName());
        ChestGUI gui = new ChestGUI(player, "§b§l我的猫娘", ChestGUI.DOUBLE_CHEST_SIZE, GUI_NEKO_NEKOS_LIST);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        if (nekos.isEmpty()) {
            gui.setItem(13, ChestGUI.createItem(Material.BARRIER, "§c没有猫娘",
                List.of("§7你名下还没有猫娘~"), "§8"), null);
        } else {
            int slot = 0;
            for (String nekoName : nekos) {
                if (slot >= 45) break;
                gui.setItem(slot, ChestGUI.createItem(Material.CAT_SPAWN_EGG, "猫娘: " + nekoName,
                    List.of("§c右键点击解除关系"), "§d"),
                    (p, click) -> openConfirmGUI(p, "解除", "§7确定要解除与猫娘 §l" + nekoName + " §c的关系吗？",
                        (confirmPlayer) -> {
                            nextNekoBridge.removeOwnerDirect(nekoName, confirmPlayer.getName());
                            confirmPlayer.sendMessage("§c呜呼~已解除与猫娘 §e" + nekoName + " §c的关系喵~");
                            openNekosListMenu(confirmPlayer);
                        },
                        (cancelPlayer) -> openNekosListMenu(cancelPlayer)));
                slot++;
            }
        }
        gui.addNavigationBar(false, false, (p, click) -> openOwnerMenu(p), null, null);
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_NEKOS_LIST);
    }

    /**
     * 收到的申请列表
     */
    public void openOwnerRequestsMenu(Player player) {
        Set<Player> requests = nextNekoBridge.getOwnerRequests(player);
        ChestGUI gui = new ChestGUI(player, "§e§l收到的申请", ChestGUI.DOUBLE_CHEST_SIZE, GUI_NEKO_REQUESTS);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        if (requests.isEmpty()) {
            gui.setItem(13, ChestGUI.createItem(Material.BARRIER, "§c暂时没有申请",
                List.of("§7暂时没有收到任何申请~"), "§8"), null);
        } else {
            int slot = 0;
            for (Player requester : requests) {
                if (slot >= 45) break;
                gui.setItem(slot, ChestGUI.createItem(Material.PAPER,
                    requester.getName() + " 想成为你的主人",
                    List.of("§a左键接受", "§c右键拒绝"), "§e"),
                    (p, click) -> {
                        if (click == ChestGUI.ClickType.RIGHT) {
                            nextNekoBridge.denyOwnerRequest(requester, p);
                            p.sendMessage("§c已拒绝 " + requester.getName() + " 的申请喵~");
                        } else {
                            nextNekoBridge.acceptOwnerRequest(requester, p);
                            p.sendMessage("§a呜呼~已接受 " + requester.getName() + " 成为你的主人！喵~");
                        }
                        openOwnerRequestsMenu(p);
                    });
                slot++;
            }
        }
        gui.addNavigationBar(false, false, (p, click) -> openOwnerMenu(p), null, null);
        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_REQUESTS);
    }

    /**
     * 管理员：全服主人与猫娘管理
     */
    public void openNekoAdminMenu(Player player) {
        if (!(player.hasPermission("nekoessentialsx.admin") || player.hasPermission("nextneko.admin"))) {
            player.sendMessage("§c呜...没有管理员权限的喵~");
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§c§l管理员管理 §7- 全服主人关系", ChestGUI.SINGLE_CHEST_SIZE, GUI_NEKO_ADMIN);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        // 选择要管理的猫娘
        gui.setItem(10, ChestGUI.createCategoryIcon(Material.PLAYER_HEAD, "选择猫娘进行管理",
            "选择全服任意猫娘，管理TA的主人关系~", "§d", true),
            (p, click) -> {
                Set<String> allNekos = nextNekoBridge.getAllNekoNames();
                List<String> names = new ArrayList<>(allNekos);
                if (names.isEmpty()) {
                    p.sendMessage("§c呜...全服还没有任何猫娘~喵~");
                    return;
                }
                openNekoNameSelector(p, "选择猫娘", names, 1, (nekoName) -> openNekoManageMenu(p, nekoName));
            });

        // 强制设置/取消猫娘
        gui.setItem(12, ChestGUI.createCategoryIcon(Material.ANVIL, "强制设置猫娘",
            "将任意玩家设置为猫娘，或取消其猫娘身份", "§e", true),
            (p, click) -> openPlayerSelector(p, "选择玩家", (target, amount) -> {
                boolean newStatus = !nextNekoBridge.isNeko(target.getName());
                nextNekoBridge.setNekoDirect(target.getName(), newStatus);
                p.sendMessage("§a呜呼~已将玩家 §e" + target.getName() + " §a设置" + (newStatus ? "为猫娘" : "为非猫娘") + "的喵~");
                NekoTitleIntegration titleIntegration = plugin.getNekoTitleIntegration();
                if (titleIntegration != null) {
                    if (newStatus) {
                        titleIntegration.grantNekoTitle(target);
                        target.sendMessage("§d§l你获得了【猫娘】头衔，可在 §e/mainmenu §d的 头衔系统 中佩戴或卸下喵~");
                    } else {
                        titleIntegration.revokeNekoTitle(target);
                    }
                }
            }));

        // 添加导航栏
        gui.addNavigationBar(false, false,
            (p, click) -> openOwnerMenu(p), null, null);

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_ADMIN);
    }

    /**
     * 管理员：管理指定猫娘的主人关系
     */
    public void openNekoManageMenu(Player player, String nekoName) {
        if (!(player.hasPermission("nekoessentialsx.admin") || player.hasPermission("nextneko.admin"))) {
            return;
        }
        ChestGUI gui = new ChestGUI(player, "§d§l管理猫娘 §7- " + nekoName, ChestGUI.SINGLE_CHEST_SIZE, GUI_NEKO_MANAGE);
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        Set<String> owners = nextNekoBridge.getOwnerNames(nekoName);
        gui.setItem(4, ChestGUI.createItem(Material.CAT_SPAWN_EGG, "§d§l" + nekoName,
            List.of("§7当前主人 (" + owners.size() + "):",
                owners.isEmpty() ? "  §7（无）" : "§e  " + String.join(", ", owners)), "§d"), null);

        // 添加主人
        gui.setItem(10, ChestGUI.createCategoryIcon(Material.GREEN_CONCRETE, "强制添加主人",
            "为这只猫娘强制添加一位主人~", "§a", true),
            (p, click) -> openPlayerSelector(p, "选择主人", (target, amount) -> {
                if (nextNekoBridge.wouldCreateCycle(nekoName, target.getName())) {
                    p.sendMessage("§c呜...会造成主人循环关系的喵~");
                    return;
                }
                nextNekoBridge.addOwnerDirect(nekoName, target.getName());
                p.sendMessage("§a呜呼~已将 §e" + target.getName() + " §a设为 §e" + nekoName + " §a的主人的喵~");
                openNekoManageMenu(p, nekoName);
            }));

        // 移除主人
        gui.setItem(12, ChestGUI.createCategoryIcon(Material.RED_CONCRETE, "强制移除主人",
            "强制解除这只猫娘的一位主人", "§c", true),
            (p, click) -> openNekoNameSelector(p, "选择要移除的主人", new ArrayList<>(owners), 1,
                (ownerName) -> {
                    nextNekoBridge.removeOwnerDirect(nekoName, ownerName);
                    p.sendMessage("§a呜呼~已强制解除 §e" + ownerName + " §a与 §e" + nekoName + " §a的关系喵~");
                    openNekoManageMenu(p, nekoName);
                }));

        // 取消猫娘身份
        gui.setItem(14, ChestGUI.createCategoryIcon(Material.COMMAND_BLOCK, "取消猫娘身份",
            "将这名玩家取消为猫娘", "§c", true),
            (p, click) -> {
                nextNekoBridge.setNekoDirect(nekoName, false);
                p.sendMessage("§c呜...已取消 §e" + nekoName + " §c的猫娘身份喵~");
                NekoTitleIntegration titleIntegration = plugin.getNekoTitleIntegration();
                if (titleIntegration != null) {
                    titleIntegration.revokeNekoTitle(nekoName);
                }
                p.closeInventory();
            });

        // 添加导航栏
        gui.addNavigationBar(false, false,
            (p, click) -> openNekoAdminMenu(p), null, null);

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEKO_MANAGE);
    }

    /**
     * 按名字选择玩家的通用选择器（支持离线玩家）
     */
    public void openNekoNameSelector(Player player, String actionName, List<String> names, int page, NekoNameCallback callback) {
        List<String> nameList = new ArrayList<>(names);
        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) nameList.size() / itemsPerPage));
        page = Math.max(1, Math.min(page, totalPages));

        ChestGUI gui = new ChestGUI(player, "§3§l" + actionName + " §7- 第 " + page + "/" + totalPages + " 页",
            ChestGUI.DOUBLE_CHEST_SIZE, GUI_NEKO_NAME_SELECTOR);
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, nameList.size());
        for (int i = startIndex; i < endIndex; i++) {
            String name = nameList.get(i);
            int slot = i - startIndex;
            final String finalName = name;
            gui.setItem(slot, ChestGUI.createItem(Material.PLAYER_HEAD, "§a§l" + name,
                List.of("§7点击选择"), "§a"),
                (p, click) -> callback.onSelect(finalName));
        }

        // 添加导航栏
        gui.setCurrentPage(page);
        gui.setMaxPage(totalPages);
        final int currentPage = page;
        final List<String> currentNames = nameList;
        gui.addNavigationBar(page > 1, page < totalPages,
            (p, click) -> goBack(p),
            (p, click) -> openNekoNameSelector(p, actionName, currentNames, currentPage - 1, callback),
            (p, click) -> openNekoNameSelector(p, actionName, currentNames, currentPage + 1, callback));

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
    }

    // ============ NextNeko 集成：NextNeko设置（管理员编辑配置文件） ============

    /**
     * 打开 NextNeko 设置GUI（管理员），可在UI中编辑 NextNeko 中只能用配置文件修改的设置
     */
    public void openNextNekoSettingsMenu(Player player) {
        if (!(player.hasPermission("nekoessentialsx.admin") || player.hasPermission("nextneko.admin"))) {
            player.sendMessage("§c呜...没有管理员权限的喵~");
            return;
        }
        if (!plugin.isNextNekoInstalled() || nextNekoBridge.getConfig() == null) {
            player.sendMessage("§c呜...检测不到 NextNeko 插件的说~喵~");
            return;
        }

        ChestGUI gui = new ChestGUI(player, "§e§lNextNeko设置 §7- 实时修改配置", ChestGUI.DOUBLE_CHEST_SIZE, GUI_NEXTNEKO_SETTINGS);
        // 填充背景
        gui.fillEmpty(Material.GRAY_STAINED_GLASS_PANE, " ");

        addConfigToggle(gui, 10, Material.NAME_TAG, "neko-chat.enabled", "猫娘聊天");
        addConfigToggle(gui, 11, Material.COOKED_BEEF, "meat-only.enabled", "只吃肉类");
        addConfigToggle(gui, 12, Material.WHEAT_SEEDS, "cat-nip.enabled", "猫薄荷");
        addConfigToggle(gui, 13, Material.IRON_SWORD, "claws.enabled", "猫爪");
        addConfigToggle(gui, 14, Material.SKELETON_SKULL, "mob-targeting.enabled", "生物目标");
        addConfigToggle(gui, 15, Material.LEATHER_CHESTPLATE, "armor-bonus.enabled", "护甲加成");
        addConfigToggle(gui, 16, Material.WITHER_ROSE, "owner-death.feature.enabled", "同生共死");
        addConfigToggle(gui, 19, Material.GOLDEN_APPLE, "health-skill.cooldown", "健康恢复冷却");
        addConfigToggle(gui, 20, Material.CLOCK, "night-effects.enabled", "夜间效果");
        addConfigToggle(gui, 21, Material.NETHERITE_SWORD, "passive-attack-boost.enabled", "被动攻击增强");
        addConfigToggle(gui, 22, Material.SHIELD, "neko-damage-modification.enabled", "猫娘伤害调整");
        addConfigToggle(gui, 23, Material.CREEPER_HEAD, "neko-mob-behavior.enabled", "猫娘生物行为");
        addConfigToggle(gui, 24, Material.LADDER, "neko-climbing.enabled", "猫娘爬墙");
        addConfigToggle(gui, 25, Material.STRING, "tail-pull.enabled", "尾巴拉扯");

        // 添加导航栏
        gui.addNavigationBar(false, false,
            (p, click) -> openMainMenu(p),
            null, null);

        gui.open();
        playerGUIs.put(player.getUniqueId(), gui);
        recordGUIHistory(player, GUI_NEXTNEKO_SETTINGS);
    }

    /**
     * 在设置GUI中注册一个可点击切换的配置项
     */
    private void addConfigToggle(ChestGUI gui, int slot, Material material, String configPath, String displayName) {
        FileConfiguration cfg = nextNekoBridge.getConfig();
        Object value = cfg.get(configPath);
        boolean current = value instanceof Boolean ? (Boolean) value : true;
        String statusText = value instanceof Boolean ? (current ? "§a开启" : "§c关闭")
                : (value != null ? "§e" + value : "§e默认");
        gui.setItem(slot, ChestGUI.createItem(material,
            (current ? "§a§l" : "§c§l") + displayName,
            List.of("§7配置路径: §f" + configPath, "§7当前: " + statusText,
                value instanceof Boolean ? "§a点击切换" : "§e点击 +10"), "§f"),
            (p, click) -> {
                toggleConfigSetting(p, configPath);
                openNextNekoSettingsMenu(p);
            });
    }

    /**
     * 切换 NextNeko 配置项（布尔值翻转，数值+10）
     */
    public void toggleConfigSetting(Player player, String configPath) {
        if (!(player.hasPermission("nekoessentialsx.admin") || player.hasPermission("nextneko.admin"))) {
            return;
        }
        FileConfiguration cfg = nextNekoBridge.getConfig();
        Object value = cfg.get(configPath);
        Object newValue;
        String newText;
        if (value instanceof Boolean) {
            newValue = !(Boolean) value;
            newText = (Boolean) newValue ? "开启" : "关闭";
        } else if (value instanceof Number) {
            long n = ((Number) value).longValue();
            newValue = n + 10;
            newText = String.valueOf(newValue);
        } else {
            player.sendMessage("§c呜...该配置项暂不支持在UI中编辑的喵~");
            return;
        }
        cfg.set(configPath, newValue);
        nextNekoBridge.saveConfig();
        nextNekoBridge.reloadConfig();
        plugin.getLogger().info("管理员 " + player.getName() + " 将 NextNeko 配置 " + configPath + " 修改为 " + newValue);
        player.sendMessage("§a呜呼~已将 §e" + configPath + " §a修改为 " + newText + " 的喵~");
    }

    // ==================== 回调接口 ====================
    
    /**
     * 玩家选择器回调接口
     */
    @FunctionalInterface
    public interface PlayerSelectorCallback {
        void onSelect(Player target, double amount);
    }
    
    /**
     * 名字选择器回调接口
     */
    @FunctionalInterface
    public interface NekoNameCallback {
        void onSelect(String name);
    }
    
    /**
     * 确认回调接口
     */
    @FunctionalInterface
    public interface ConfirmCallback {
        void onConfirm(Player player);
        default void onCancel(Player player) {}
    }
}

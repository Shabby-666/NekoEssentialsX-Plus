package com.nekoessentialsx.gui;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import com.nekoessentialsx.economy.EconomyManager;
import com.nekoessentialsx.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIManager {
    private final NekoEssentialX plugin;
    private final TitleManager titleManager;
    private final EconomyManager economyManager;
    private final DatabaseManager databaseManager;
    private final Map<String, Inventory> guis = new HashMap<>();
    private final Map<UUID, GUISession> playerSessions = new HashMap<>();
    private ChatInputListener chatInputListener;
    
    // 用于按钮标识的 PDC key（跨版本可靠，不再依赖 LocalizedName）
    private final NamespacedKey buttonKey;
    
    // GUI常量
    public static final int PAGE_SIZE = 10;
    public static final int TITLE_COST = 100;
    public static final int CUSTOM_TITLE_COST = 1000;
    
    // GUI类型
    public enum GUIType {
        MAIN, 
        TITLE_SHOP, 
        PERSONAL_TITLES, 
        CREATE_CUSTOM_TITLE, 
        ADMIN_TITLE_MANAGER,
        TP_MENU,
        ECONOMY_MENU,
        PLUGIN_MENU,
        TP_REQUESTS,
        ECONOMY_BALANCE,
        ECONOMY_PAY,
        ECONOMY_ADMIN
    }
    
    // 玩家会话类
    public static class GUISession {
        private final Player player;
        private GUIType currentType;
        private int currentPage;
        private Map<String, Object> data;
        
        public GUISession(Player player) {
            this.player = player;
            this.currentType = GUIType.MAIN;
            this.currentPage = 1;
            this.data = new HashMap<>();
        }
        
        public Player getPlayer() { return player; }
        public GUIType getCurrentType() { return currentType; }
        public void setCurrentType(GUIType currentType) { this.currentType = currentType; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public Map<String, Object> getData() { return data; }
        public void setData(String key, Object value) { this.data.put(key, value); }
        public void clear() {
            this.currentType = GUIType.MAIN;
            this.currentPage = 1;
            this.data.clear();
        }
    }
    
    // GUI项类型
    public enum GUIItemType {
        TITLE_SHOP_ITEM, 
        PERSONAL_TITLE_ITEM, 
        PAGE_NEXT, 
        PAGE_PREV, 
        PAGE_FIRST, 
        PAGE_LAST, 
        CUSTOM_TITLE_CREATE, 
        ADMIN_TITLE_MANAGER,
        ADMIN_CREATE_TITLE, 
        ADMIN_EDIT_TITLE, 
        ADMIN_DELETE_TITLE,
        BACK_TO_MAIN,
        CONFIRM_CREATE,
        CANCEL_CREATE,
        TP_MENU_ITEM,
        ECONOMY_MENU_ITEM,
        PLUGIN_MENU_ITEM,
        TP_REQUESTS_ITEM,
        ECONOMY_BALANCE_ITEM,
        ECONOMY_PAY_ITEM,
        ECONOMY_ADMIN_ITEM,
        TP_SEND_REQUEST,
        TP_ACCEPT_REQUEST,
        TP_DENY_REQUEST,
        TP_CANCEL_REQUEST,
        ECONOMY_VIEW_BALANCE,
        ECONOMY_SEND_MONEY,
        ECONOMY_ADMIN_DEPOSIT,
        ECONOMY_ADMIN_WITHDRAW,
        ECONOMY_ADMIN_GIVE,
        ECONOMY_ADMIN_TAKE,
        PLUGIN_RELOAD,
        PLUGIN_VERSION,
        CONFIRM_ACTION,
        CANCEL_ACTION,
        NEWBIE_GIFT_ITEM
    }
    
    public GUIManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        this.economyManager = plugin.getEconomyManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.buttonKey = new NamespacedKey(plugin, "gui_button");
    }
    
    /**
     * 初始化GUI管理器
     */
    public void initialize() {
        // 创建聊天输入监听器
        this.chatInputListener = new ChatInputListener(plugin, this);
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(new GUIListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(chatInputListener, plugin);
    }
    
    /**
     * 创建GUI界面
     */
    public Inventory createGUI(String title, int size) {
        Inventory gui = plugin.getServer().createInventory(new GUIHolder(), size, title);
        guis.put(title, gui);
        return gui;
    }
    
    /**
     * 创建GUI界面
     */
    public Inventory createGUI(String title, InventoryType type) {
        Inventory gui = plugin.getServer().createInventory(new GUIHolder(), type, title);
        guis.put(title, gui);
        return gui;
    }
    
    /**
     * 打开GUI界面
     */
    public void openGUI(Player player, Inventory gui) {
        player.openInventory(gui);
    }
    
    /**
     * 关闭GUI界面
     */
    public void closeGUI(Player player) {
        player.closeInventory();
    }
    
    /**
     * 获取或创建玩家会话
     */
    public GUISession getOrCreateSession(Player player) {
        return playerSessions.computeIfAbsent(player.getUniqueId(), k -> new GUISession(player));
    }
    
    /**
     * 清理玩家会话
     */
    public void clearSession(Player player) {
        playerSessions.remove(player.getUniqueId());
    }
    
    /**
     * 创建导航按钮
     */
    public ItemStack createNavigationItem(Material material, String name, List<String> lore, GUIItemType type) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            // 设置自定义数据，用于事件处理
            setButtonData(meta, type.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 设置按钮标识（写入 PDC，跨版本可靠）
     */
    public void setButtonData(ItemMeta meta, String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, value);
        }
    }

    /**
     * 获取按钮标识（从 PDC 读取，跨版本可靠）
     */
    public String getButtonData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(buttonKey, PersistentDataType.STRING);
    }
    
    /**
     * 创建标题项
     */
    public ItemStack createTitleItem(String titleId, String titleName, String titlePrefix, int cost, boolean owned, boolean equipped) {
        Material material = owned ? Material.DIAMOND : Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = (equipped ? "§a[已装备] " : "") + "§b" + titleName;
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7前缀: " + titlePrefix);
            lore.add("§7ID: " + titleId);
            if (!owned) {
                lore.add("§6价格: " + cost + " " + economyManager.getCurrencyName());
                lore.add("§a点击购买");
            } else if (equipped) {
                lore.add("§e当前已装备");
                lore.add("§c点击卸下");
            } else {
                lore.add("§a点击装备");
            }
            
            meta.setLore(lore);
            setButtonData(meta, titleId);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 打开主菜单
     */
    public void openMainGUI(Player player) {
        Inventory mainGUI = createGUI("§6NekoEssentialsX+ Main Menu", 54);
        
        // 创建头衔系统按钮
        ItemStack shopItem = createNavigationItem(
            Material.CHEST, 
            "§a头衔商店", 
            List.of("§7查看和购买头衔", "§7每个头衔: " + TITLE_COST + " " + economyManager.getCurrencyName()),
            GUIItemType.TITLE_SHOP_ITEM
        );
        mainGUI.setItem(10, shopItem);
        
        // 创建个人头衔管理按钮
        ItemStack personalItem = createNavigationItem(
            Material.PLAYER_HEAD, 
            "§b个人头衔", 
            List.of("§7管理你已拥有的头衔", "§7装备或卸下头衔"),
            GUIItemType.PERSONAL_TITLE_ITEM
        );
        mainGUI.setItem(11, personalItem);
        
        // 创建自定义头衔创建按钮
        ItemStack customItem = createNavigationItem(
            Material.ANVIL, 
            "§d创建自定义头衔", 
            List.of("§7创建你自己的专属头衔", "§6价格: " + CUSTOM_TITLE_COST + " " + economyManager.getCurrencyName()),
            GUIItemType.CUSTOM_TITLE_CREATE
        );
        mainGUI.setItem(12, customItem);
        
        // 创建传送系统菜单按钮
        ItemStack tpItem = createNavigationItem(
            Material.ENDER_PEARL, 
            "§9传送系统", 
            List.of("§7管理传送请求", "§7发送、接受、拒绝传送"),
            GUIItemType.TP_MENU_ITEM
        );
        mainGUI.setItem(14, tpItem);
        
        // 创建经济系统菜单按钮
        ItemStack ecoItem = createNavigationItem(
            Material.GOLD_INGOT, 
            "§6经济系统", 
            List.of("§7查看余额", "§7发送金钱给其他玩家", "§7管理你的经济"),
            GUIItemType.ECONOMY_MENU_ITEM
        );
        mainGUI.setItem(15, ecoItem);
        
        // 创建插件管理菜单按钮
        ItemStack pluginItem = createNavigationItem(
            Material.PAPER, 
            "§f插件管理", 
            List.of("§7重载插件配置", "§7查看插件版本", "§7插件信息"),
            GUIItemType.PLUGIN_MENU_ITEM
        );
        mainGUI.setItem(16, pluginItem);
        
        // 如果是管理员，添加管理员按钮
        if (player.hasPermission("nekoessentialsx.title.admin")) {
            ItemStack adminItem = createNavigationItem(
                Material.COMMAND_BLOCK, 
                "§c管理员头衔管理", 
                List.of("§7创建、编辑和删除头衔", "§7仅管理员可用"),
                GUIItemType.ADMIN_TITLE_MANAGER
            );
            mainGUI.setItem(28, adminItem);
        }
        
        openGUI(player, mainGUI);
    }
    
    /**
     * 打开头衔商店界面
     */
    public void openTitleShop(Player player, int page) {
        GUISession session = getOrCreateSession(player);
        session.setCurrentType(GUIType.TITLE_SHOP);
        session.setCurrentPage(page);
        
        Inventory shopGUI = createGUI("§a头衔商店 - 第" + page + "页", 54);
        
        // 获取所有头衔
        java.util.Map<String, TitleManager.Title> allTitles = titleManager.getTitles();
        java.util.List<String> titleIds = new java.util.ArrayList<>(allTitles.keySet());
        
        // 计算分页
        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, titleIds.size());
        
        // 显示头衔
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = titleIds.get(i);
            TitleManager.Title title = allTitles.get(titleId);
            if (title != null && title.isEnabled()) {
                boolean owned = databaseManager.hasTitle(player.getName(), titleId);
                boolean equipped = titleId.equals(titleManager.getPlayerTitle(player.getName()));
                ItemStack item = createTitleItem(titleId, title.getName(), title.getPrefix(), TITLE_COST, owned, equipped);
                shopGUI.setItem(i - startIndex, item);
            }
        }
        
        // 添加分页按钮
        if (endIndex < titleIds.size()) {
            ItemStack nextPage = createNavigationItem(
                Material.ARROW, 
                "§a下一页 >", 
                java.util.List.of("§7前往第" + (page + 1) + "页"),
                GUIItemType.PAGE_NEXT
            );
            shopGUI.setItem(53, nextPage);
        }
        
        if (startIndex > 0) {
            ItemStack prevPage = createNavigationItem(
                Material.ARROW, 
                "§a< 上一页", 
                java.util.List.of("§7前往第" + (page - 1) + "页"),
                GUIItemType.PAGE_PREV
            );
            shopGUI.setItem(45, prevPage);
        }
        
        // 添加返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.BARRIER, 
            "§c返回主菜单", 
            java.util.List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        shopGUI.setItem(49, backButton);
        
        openGUI(player, shopGUI);
    }
    
    /**
     * 打开个人头衔界面
     */
    public void openPersonalTitles(Player player, int page) {
        GUISession session = getOrCreateSession(player);
        session.setCurrentType(GUIType.PERSONAL_TITLES);
        session.setCurrentPage(page);
        
        Inventory personalGUI = createGUI("§b个人头衔 - 第" + page + "页", 54);
        
        // 获取玩家已拥有的所有头衔
        java.util.List<String> ownedTitles = new java.util.ArrayList<>();
        
        // 添加系统头衔
        for (String titleId : titleManager.getTitles().keySet()) {
            if (databaseManager.hasTitle(player.getName(), titleId)) {
                ownedTitles.add(titleId);
            }
        }
        
        // 添加自定义头衔
        java.util.List<String> customTitleIds = databaseManager.getPlayerCustomTitles(player.getName());
        for (String customTitleId : customTitleIds) {
            if (databaseManager.hasTitle(player.getName(), customTitleId)) {
                ownedTitles.add(customTitleId);
            }
        }
        
        // 计算分页
        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, ownedTitles.size());
        
        // 显示头衔
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = ownedTitles.get(i);
            
            // 获取头衔信息
            TitleManager.Title systemTitle = titleManager.getTitle(titleId);
            Object[] customTitleData = databaseManager.getCustomTitle(titleId);
            
            String titleName;
            String prefix;
            boolean enabled = true;
            
            if (systemTitle != null) {
                // 系统头衔
                titleName = systemTitle.getName();
                prefix = systemTitle.getPrefix();
                enabled = systemTitle.isEnabled();
            } else if (customTitleData != null) {
                // 自定义头衔
                titleName = (String) customTitleData[0];
                prefix = (String) customTitleData[1];
                enabled = (boolean) customTitleData[4];
            } else {
                // 头衔不存在，跳过
                continue;
            }
            
            if (enabled) {
                boolean equipped = titleId.equals(titleManager.getPlayerTitle(player.getName()));
                ItemStack item = createTitleItem(titleId, titleName, prefix, TITLE_COST, true, equipped);
                personalGUI.setItem(i - startIndex, item);
            }
        }
        
        // 添加分页按钮
        if (endIndex < ownedTitles.size()) {
            ItemStack nextPage = createNavigationItem(
                Material.ARROW, 
                "§a下一页 >", 
                List.of("§7前往第" + (page + 1) + "页"),
                GUIItemType.PAGE_NEXT
            );
            personalGUI.setItem(53, nextPage);
        }
        
        if (startIndex > 0) {
            ItemStack prevPage = createNavigationItem(
                Material.ARROW, 
                "§a< 上一页", 
                List.of("§7前往第" + (page - 1) + "页"),
                GUIItemType.PAGE_PREV
            );
            personalGUI.setItem(45, prevPage);
        }
        
        // 添加返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.BARRIER, 
            "§c返回主菜单", 
            List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        personalGUI.setItem(49, backButton);
        
        openGUI(player, personalGUI);
    }
    
    /**
     * 处理头衔购买
     */
    public void handleTitlePurchase(Player player, String titleId) {
        // 检查头衔是否存在
        TitleManager.Title title = titleManager.getTitle(titleId);
        if (title == null || !title.isEnabled()) {
            player.sendMessage("§c呜...这个头衔不存在，或者已经被关掉啦的说~喵~");
            return;
        }
        
        // 检查是否已经拥有
        if (databaseManager.hasTitle(player.getName(), titleId)) {
            player.sendMessage("§e主人已经拥有这个头衔啦的说~喵~");
            return;
        }
        
        // 检查余额是否足够
        double balance = economyManager.getBalance(player);
        if (balance < TITLE_COST) {
            player.sendMessage("§c呜...你的余额不足的说！需要 " + TITLE_COST + " " + economyManager.getCurrencyName() + "，你现在只有 " + (int) balance + " " + economyManager.getCurrencyName() + " 喵~");
            return;
        }
        
        // 扣除余额
        if (economyManager.withdrawPlayer(player, TITLE_COST)) {
            // 添加头衔到玩家仓库
            databaseManager.addTitleToInventory(player.getName(), titleId, false);
            
            // 发送成功消息
            player.sendMessage("§a呜呼~买到啦！主人获得头衔: §b" + title.getName() + " §a 的说~喵~");
            player.sendMessage("§a已扣除 " + TITLE_COST + " " + economyManager.getCurrencyName() + "，你当前余额: §6" + (int) (balance - TITLE_COST) + " " + economyManager.getCurrencyName());
            
            // 重新打开商店界面，更新状态
            openTitleShop(player, 1);
        } else {
            player.sendMessage("§c呜...购买失败了的说，等一小会儿再试试吧喵~");
        }
    }
    
    /**
     * 处理头衔装备
     */
    public void handleTitleEquip(Player player, String titleId) {
        // 检查玩家是否拥有该头衔
        if (!databaseManager.hasTitle(player.getName(), titleId)) {
            player.sendMessage("§c呜...主人还没有这个头衔的说~喵~");
            return;
        }
        
        // 获取当前装备的头衔
        String currentTitleId = titleManager.getPlayerTitle(player.getName());
        
        // 如果当前装备的是同一个头衔，则卸下
        if (titleId.equals(currentTitleId)) {
            handleTitleUnequip(player);
            return;
        }
        
        // 获取头衔信息
        TitleManager.Title systemTitle = titleManager.getTitle(titleId);
        Object[] customTitleData = databaseManager.getCustomTitle(titleId);
        
        String titleName;
        
        if (systemTitle != null) {
            // 系统头衔
            titleName = systemTitle.getName();
        } else if (customTitleData != null) {
            // 自定义头衔
            titleName = (String) customTitleData[0];
        } else {
            player.sendMessage("§c呜...这个头衔不存在，或者已经被删掉的说~喵~");
            return;
        }
        
        // 装备新头衔
        titleManager.updatePlayerTitle(player, titleId);
        player.sendMessage("§a呜呼~头衔戴好啦！主人的头衔现在是: §b" + titleName + " §a 的说~喵~");
        
        // 重新打开当前界面，更新状态
        openPersonalTitles(player, 1);
    }
    
    /**
     * 处理头衔卸下
     */
    public void handleTitleUnequip(Player player) {
        titleManager.clearPlayerTitle(player.getName());
        player.sendMessage("§a呜呼~头衔卸下成功啦喵~");
        
        // 重新打开当前界面，更新状态
        openPersonalTitles(player, 1);
    }
    
    /**
     * 打开管理员头衔管理界面
     */
    public void openAdminTitleManager(Player player, int page) {
        if (!player.hasPermission("nekoessentialsx.title.admin")) {
            player.sendMessage("§c呜...主人没有权限用这个功能的说~喵~");
            return;
        }
        
        GUISession session = getOrCreateSession(player);
        session.setCurrentType(GUIType.ADMIN_TITLE_MANAGER);
        session.setCurrentPage(page);
        
        Inventory adminGUI = createGUI("§c管理员头衔管理 - 第" + page + "页", 54);
        
        // 获取所有头衔
        java.util.Map<String, TitleManager.Title> allTitles = titleManager.getTitles();
        java.util.List<String> titleIds = new java.util.ArrayList<>(allTitles.keySet());
        
        // 计算分页
        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, titleIds.size());
        
        // 显示头衔
        for (int i = startIndex; i < endIndex; i++) {
            String titleId = titleIds.get(i);
            TitleManager.Title title = allTitles.get(titleId);
            if (title != null) {
                ItemStack item = createAdminTitleItem(titleId, title.getName(), title.getPrefix(), title.isEnabled());
                adminGUI.setItem(i - startIndex, item);
            }
        }
        
        // 添加分页按钮
        if (endIndex < titleIds.size()) {
            ItemStack nextPage = createNavigationItem(
                Material.ARROW, 
                "§a下一页 >", 
                java.util.List.of("§7前往第" + (page + 1) + "页"),
                GUIItemType.PAGE_NEXT
            );
            adminGUI.setItem(53, nextPage);
        }
        
        if (startIndex > 0) {
            ItemStack prevPage = createNavigationItem(
                Material.ARROW, 
                "§a< 上一页", 
                java.util.List.of("§7前往第" + (page - 1) + "页"),
                GUIItemType.PAGE_PREV
            );
            adminGUI.setItem(45, prevPage);
        }
        
        // 添加创建新头衔按钮
        ItemStack createTitle = createNavigationItem(
            Material.GOLD_BLOCK, 
            "§a创建新头衔", 
            java.util.List.of("§7点击创建一个新的系统头衔"),
            GUIItemType.ADMIN_CREATE_TITLE
        );
        adminGUI.setItem(51, createTitle);
        
        // 添加返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.BARRIER, 
            "§c返回主菜单", 
            java.util.List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        adminGUI.setItem(49, backButton);
        
        openGUI(player, adminGUI);
    }
    
    /**
     * 创建管理员头衔项
     */
    public ItemStack createAdminTitleItem(String titleId, String titleName, String titlePrefix, boolean enabled) {
        Material material = enabled ? Material.DIAMOND_BLOCK : Material.COAL_BLOCK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = (enabled ? "§a[启用] " : "§c[禁用] ") + "§b" + titleName;
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7前缀: " + titlePrefix);
            lore.add("§7ID: " + titleId);
            lore.add("§7状态: " + (enabled ? "§a启用" : "§c禁用"));
            lore.add("§a左键编辑");
            lore.add("§c右键删除");
            lore.add("§eShift+左键切换状态");
            
            meta.setLore(lore);
            setButtonData(meta, titleId);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 处理管理员创建头衔
     */
    public void handleAdminCreateTitle(Player player, String titleId, String titleName, String prefix, int priority, boolean enabled) {
        if (!player.hasPermission("nekoessentialsx.title.admin")) {
            player.sendMessage("§c呜...主人没有权限用这个功能的说~喵~");
            return;
        }
        
        titleManager.createTitle(titleId, titleName, prefix, "", "nekoessentialsx.titles." + titleId, priority, enabled);
        player.sendMessage("§a呜呼~头衔创建成功啦喵~");
        player.sendMessage("§a头衔ID: §b" + titleId);
        player.sendMessage("§a头衔名称: §b" + titleName);
    }
    
    /**
     * 处理管理员删除头衔
     */
    public void handleAdminDeleteTitle(Player player, String titleId) {
        if (!player.hasPermission("nekoessentialsx.title.admin")) {
            player.sendMessage("§c呜...主人没有权限用这个功能的说~喵~");
            return;
        }
        
        titleManager.deleteTitle(titleId);
        player.sendMessage("§a呜呼~头衔删除成功啦喵~");
    }
    
    /**
     * 处理管理员编辑头衔
     */
    public void handleAdminEditTitle(Player player, String titleId, String titleName, String prefix, int priority, boolean enabled) {
        if (!player.hasPermission("nekoessentialsx.title.admin")) {
            player.sendMessage("§c呜...主人没有权限用这个功能的说~喵~");
            return;
        }
        
        titleManager.editTitle(titleId, titleName, prefix, "", "nekoessentialsx.titles." + titleId, priority, enabled);
        player.sendMessage("§a呜呼~头衔编辑成功啦喵~");
    }

    /**
     * 打开自定义头衔确认界面
     */
    /**
     * 检查自定义头衔ID是否已被占用（系统头衔或数据库中的自定义头衔）
     */
    public boolean isCustomTitleIdTaken(Player player, String titleId) {
        if (titleId == null || titleId.isEmpty()) {
            return true;
        }
        if (titleManager.getTitle(titleId) != null) {
            return true;
        }
        return databaseManager.getCustomTitle(titleId) != null;
    }

    /**
     * 打开自定义头衔确认界面
     */
    public void openCreateTitleConfirmGUI(Player player, String titleId, String titleName) {
        Inventory confirmGUI = createGUI("§d确认创建自定义头衔", 27);
        
        // 创建标题信息项
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta meta = infoItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e头衔创建确认");
            List<String> lore = new ArrayList<>();
            lore.add("§7你确定要创建以下头衔吗？");
            lore.add("§7头衔ID: §b" + titleId);
            lore.add("§7头衔名称: §b" + titleName);
            lore.add("§7头衔前缀: §b[" + titleName + "] ");
            lore.add("§7价格: " + (player.hasPermission("nekoessentialsx.title.admin") ? "§a免费" : "§6" + CUSTOM_TITLE_COST + " " + economyManager.getCurrencyName()));
            lore.add("§a点击确认按钮创建");
            lore.add("§c点击取消按钮放弃");
            meta.setLore(lore);
            infoItem.setItemMeta(meta);
        }
        confirmGUI.setItem(13, infoItem);
        
        // 创建确认按钮
        ItemStack confirmButton = createNavigationItem(
            Material.GREEN_CONCRETE,
            "§a确认创建",
            List.of("§7确认创建这个自定义头衔"),
            GUIItemType.CONFIRM_CREATE
        );
        confirmGUI.setItem(11, confirmButton);
        
        // 创建取消按钮
        ItemStack cancelButton = createNavigationItem(
            Material.RED_CONCRETE,
            "§c取消",
            List.of("§7取消创建这个自定义头衔"),
            GUIItemType.CANCEL_CREATE
        );
        confirmGUI.setItem(15, cancelButton);
        
        // 保存标题信息到会话数据
        GUISession session = getOrCreateSession(player);
        session.setData("titleId", titleId);
        session.setData("titleName", titleName);
        
        openGUI(player, confirmGUI);
    }
    
    /**
     * 处理确认创建自定义头衔
     */
    public void handleConfirmCreateTitle(Player player) {
        GUISession session = getOrCreateSession(player);
        String titleId = (String) session.getData().get("titleId");
        String titleName = (String) session.getData().get("titleName");
        if (titleId == null || titleName == null) {
            player.sendMessage("§c呜...没能拿到头衔信息的说，重新开始创建吧喵~");
            return;
        }
        
        // 再次确认ID未被占用，避免并发冲突
        if (isCustomTitleIdTaken(player, titleId)) {
            player.sendMessage("§c呜...头衔ID已经被占用的说，请换一个ID重新创建吧喵~");
            player.sendMessage("§e如果你想继续创建，请关闭界面后重新输入" + " <id> <name> 的说~喵~");
            player.closeInventory();
            return;
        }
        
        String prefix = "[" + titleName + "] ";
        
        // 检查权限和余额
        boolean isAdmin = player.hasPermission("nekoessentialsx.title.admin");
        int cost = isAdmin ? 0 : CUSTOM_TITLE_COST;
        
        if (!isAdmin) {
            double balance = economyManager.getBalance(player);
            if (balance < cost) {
                player.sendMessage("§c呜...主人的余额不足啦！需要 " + cost + " " + economyManager.getCurrencyName() + ", 你现在只有 " + (int) balance + " " + economyManager.getCurrencyName() + " 喵~");
                return;
            }
        }
        
        // 扣除余额（非管理员）
        if (!isAdmin) {
            if (!economyManager.withdrawPlayer(player, cost)) {
                player.sendMessage("§c呜...创建失败了的说，等一小会儿再试试吧喵~");
                return;
            }
        }
        
        // 保存自定义头衔
        if (databaseManager.saveCustomTitle(titleId, player.getName(), titleName, prefix, "")) {
            // 添加到玩家仓库
            databaseManager.addTitleToInventory(player.getName(), titleId, true);
            
            // 发送成功消息
            player.sendMessage("§a呜呼~自定义头衔创建成功啦喵~");
            player.sendMessage("§a头衔ID: §b" + titleId);
            player.sendMessage("§a头衔名称: §b" + titleName);
            player.sendMessage("§a头衔前缀: §b" + prefix);
            if (!isAdmin) {
                player.sendMessage("§a已扣除 " + cost + " " + economyManager.getCurrencyName());
            }
            
            // 打开个人头衔界面
            openPersonalTitles(player, 1);
        } else {
            player.sendMessage("§c呜...创建失败了的说，等一小会儿再试试吧喵~");
        }
    }
    
    /**
     * 处理取消创建自定义头衔
     */
    public void handleCancelCreateTitle(Player player) {
        player.sendMessage("§e呜...自定义头衔创建被取消啦，下次再来玩吧喵~");
        // 返回到主菜单或个人头衔界面
        openMainGUI(player);
    }
    
    /**
     * 打开传送系统菜单
     */
    public void openTPMenu(Player player) {
        Inventory tpMenu = createGUI("§9传送系统", 27);
        
        // 创建发送传送请求按钮
        ItemStack sendRequestItem = createNavigationItem(
            Material.PAPER, 
            "§a发送传送请求", 
            List.of("§7发送传送请求给其他玩家", "§7格式：/tpa <player>"),
            GUIItemType.TP_SEND_REQUEST
        );
        tpMenu.setItem(10, sendRequestItem);
        
        // 创建查看传送请求按钮
        ItemStack viewRequestsItem = createNavigationItem(
            Material.ENCHANTED_BOOK, 
            "§b查看传送请求", 
            List.of("§7查看你收到的传送请求", "§7接受或拒绝请求"),
            GUIItemType.TP_REQUESTS_ITEM
        );
        tpMenu.setItem(13, viewRequestsItem);
        
        // 创建取消传送请求按钮
        ItemStack cancelRequestItem = createNavigationItem(
            Material.BARRIER, 
            "§c取消传送请求", 
            List.of("§7取消你发送的传送请求", "§7格式：/tpacancel [player]"),
            GUIItemType.TP_CANCEL_REQUEST
        );
        tpMenu.setItem(16, cancelRequestItem);
        
        // 创建返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.RED_BED, 
            "§c返回主菜单", 
            List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        tpMenu.setItem(22, backButton);
        
        openGUI(player, tpMenu);
    }
    
    /**
     * 打开经济系统菜单
     */
    public void openEconomyMenu(Player player) {
        Inventory economyMenu = createGUI("§6经济系统", 27);
        
        // 创建查看余额按钮
        ItemStack balanceItem = createNavigationItem(
            Material.GOLD_NUGGET, 
            "§a查看余额", 
            List.of("§7查看你的当前余额", "§7格式：/money balance"),
            GUIItemType.ECONOMY_VIEW_BALANCE
        );
        economyMenu.setItem(10, balanceItem);
        
        // 创建发送金钱按钮
        ItemStack sendItem = createNavigationItem(
            Material.GOLD_INGOT, 
            "§6发送金钱", 
            List.of("§7发送金钱给其他玩家", "§7格式：/money pay <player> <amount>"),
            GUIItemType.ECONOMY_SEND_MONEY
        );
        economyMenu.setItem(13, sendItem);
        
        // 创建查看其他玩家余额按钮
        ItemStack viewOtherBalanceItem = createNavigationItem(
            Material.PAPER, 
            "§b查看其他玩家余额", 
            List.of("§7查看其他玩家的当前余额", "§7格式：/money balance <player>"),
            GUIItemType.ECONOMY_ADMIN_ITEM
        );
        economyMenu.setItem(16, viewOtherBalanceItem);
        
        // 创建管理员经济管理菜单按钮
        if (player.hasPermission("nekoessentialsx.economy.admin")) {
            ItemStack adminMenuItem = createNavigationItem(
                Material.COMMAND_BLOCK, 
                "§c管理员经济管理", 
                List.of("§7管理其他玩家的经济", "§7充值、扣款、给予、扣除"),
                GUIItemType.ECONOMY_ADMIN_ITEM
            );
            economyMenu.setItem(20, adminMenuItem);
            
            // 添加更改货币名称按钮
            ItemStack currencyNameItem = createNavigationItem(
                Material.NAME_TAG, 
                "§d更改货币名称", 
                List.of("§7更改游戏中的货币名称", "§7当前货币名称：" + economyManager.getCurrencyName()),
                GUIItemType.ECONOMY_ADMIN_DEPOSIT
            );
            economyMenu.setItem(24, currencyNameItem);
        }
        
        // 创建返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.RED_BED, 
            "§c返回主菜单", 
            List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        economyMenu.setItem(22, backButton);
        
        openGUI(player, economyMenu);
    }
    
    /**
     * 打开管理员经济管理界面
     */
    public void openEconomyAdminGUI(Player player) {
        Inventory adminGUI = createGUI("§c管理员经济管理", 27);
        
        // 创建充值按钮
        ItemStack depositItem = createNavigationItem(
            Material.GREEN_CONCRETE, 
            "§a充值", 
            List.of("§7为玩家充值", "§7格式：/money give <player> <amount>"),
            GUIItemType.ECONOMY_ADMIN_DEPOSIT
        );
        adminGUI.setItem(10, depositItem);
        
        // 创建扣款按钮
        ItemStack withdrawItem = createNavigationItem(
            Material.RED_CONCRETE, 
            "§c扣款", 
            List.of("§7从玩家账户扣款", "§7格式：/money take <player> <amount>"),
            GUIItemType.ECONOMY_ADMIN_WITHDRAW
        );
        adminGUI.setItem(12, withdrawItem);
        
        // 创建设置余额按钮
        ItemStack setItem = createNavigationItem(
            Material.YELLOW_CONCRETE, 
            "§e设置余额", 
            List.of("§7直接设置玩家余额", "§7格式：/money set <player> <amount>"),
            GUIItemType.ECONOMY_ADMIN_ITEM
        );
        adminGUI.setItem(14, setItem);
        
        // 创建查看其他玩家余额按钮
        ItemStack viewItem = createNavigationItem(
            Material.PAPER, 
            "§b查看其他玩家余额", 
            List.of("§7查看其他玩家的当前余额", "§7格式：/money balance <player>"),
            GUIItemType.ECONOMY_VIEW_BALANCE
        );
        adminGUI.setItem(16, viewItem);
        
        // 创建返回按钮
        ItemStack backButton = createNavigationItem(
            Material.RED_BED, 
            "§c返回经济系统", 
            List.of("§7返回经济系统菜单"),
            GUIItemType.BACK_TO_MAIN
        );
        adminGUI.setItem(22, backButton);
        
        openGUI(player, adminGUI);
    }
    
    /**
     * 打开插件管理菜单
     */
    public void openPluginMenu(Player player) {
        Inventory pluginMenu = createGUI("§f插件管理", 27);
        
        // 创建查看余额按钮
        ItemStack versionItem = createNavigationItem(
            Material.BOOK, 
            "§a查看版本", 
            List.of("§7查看插件当前版本", "§7格式：/nekoessentialx version"),
            GUIItemType.PLUGIN_VERSION
        );
        pluginMenu.setItem(10, versionItem);
        
        // 创建发送金钱按钮
        ItemStack reloadItem = createNavigationItem(
            Material.COMMAND_BLOCK, 
            "§6重载配置", 
            List.of("§7重载插件配置文件", "§7格式：/nekoessentialx reload"),
            GUIItemType.PLUGIN_RELOAD
        );
        pluginMenu.setItem(13, reloadItem);
        
        // 创建返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.RED_BED, 
            "§c返回主菜单", 
            List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        pluginMenu.setItem(22, backButton);
        
        openGUI(player, pluginMenu);
    }
    
    public static class GUIHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
    
    /**
     * 获取聊天输入监听器实例
     * @return ChatInputListener实例
     */
    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }
    
    /**
     * 获取插件实例
     * @return 插件实例
     */
    public NekoEssentialX getPlugin() {
        return plugin;
    }
    
    /**
     * 打开新手礼包GUI
     * @param player 玩家对象
     */
    public void openNewbieGiftGUI(Player player) {
        // 创建单排行容器（9格）
        Inventory newbieGiftGUI = createGUI("§d新手大礼包", 9);
        
        // 获取随机物品用于显示
        com.nekoessentialsx.newbiegift.NewbieGiftManager.GiftItem randomGiftItem = plugin.getNewbieGiftManager().getRandomGiftItem();
        Material displayMaterial = Material.DIAMOND_CHESTPLATE;
        if (randomGiftItem != null) {
            displayMaterial = randomGiftItem.getMaterial();
        }
        
        // 创建新手大礼包物品
        ItemStack newbieGiftItem = createNavigationItem(
            displayMaterial,
            "§6新手大礼包",
            List.of(
                "§7点击领取你的新手大礼包",
                "§7包含随机15件物品",
                "§a点击领取"
            ),
            GUIItemType.NEWBIE_GIFT_ITEM
        );
        
        // 放置在中央位置（第5格）
        newbieGiftGUI.setItem(4, newbieGiftItem);
        
        openGUI(player, newbieGiftGUI);
    }
    
    /**
     * 打开传送请求GUI
     * @param player 玩家对象
     */
    public void openTPRequestsGUI(Player player) {
        GUISession session = getOrCreateSession(player);
        session.setCurrentType(GUIType.TP_REQUESTS);
        session.setCurrentPage(1);
        
        Inventory tpRequestsGUI = createGUI("§9传送请求管理", 54);
        
        // 获取玩家收到的传送请求
        java.util.List<com.nekoessentialsx.tpa.TPAManager.TPARequest> requests = plugin.getTPAManager().getReceivedRequests(player);
        
        // 显示传送请求
        int slot = 0;
        for (com.nekoessentialsx.tpa.TPAManager.TPARequest request : requests) {
            if (slot >= 54) {
                break; // 超出GUI容量，不再显示
            }
            
            Player sender = request.getSender();
            ItemStack requestItem = new ItemStack(Material.PAPER);
            ItemMeta meta = requestItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + sender.getName() + " §7请求传送");
                List<String> lore = new ArrayList<>();
                lore.add("§7点击接受传送请求");
                lore.add("§a左键接受");
                lore.add("§c右键拒绝");
                meta.setLore(lore);
                setButtonData(meta, "tp_request_" + sender.getName());
                requestItem.setItemMeta(meta);
            }
            tpRequestsGUI.setItem(slot, requestItem);
            slot++;
        }
        
        // 添加返回主菜单按钮
        ItemStack backButton = createNavigationItem(
            Material.RED_BED, 
            "§c返回主菜单", 
            List.of("§7返回主菜单界面"),
            GUIItemType.BACK_TO_MAIN
        );
        tpRequestsGUI.setItem(49, backButton);
        
        openGUI(player, tpRequestsGUI);
    }
}
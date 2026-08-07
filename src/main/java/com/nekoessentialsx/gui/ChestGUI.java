package com.nekoessentialsx.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 箱子GUI核心类
 * 模拟Minecraft箱子界面风格
 */
public class ChestGUI {
    
    // GUI尺寸常量
    public static final int ROW_SIZE = 9;
    public static final int SINGLE_CHEST_SIZE = 27;
    public static final int DOUBLE_CHEST_SIZE = 54;
    
    // 分隔栏位置（箱子底部）
    public static final int[] NAVIGATION_SLOTS = {45, 46, 47, 48, 49, 50, 51, 52, 53};
    public static final int BACK_BUTTON_SLOT = 49;
    public static final int PREV_PAGE_SLOT = 48;
    public static final int NEXT_PAGE_SLOT = 50;
    public static final int INFO_SLOT = 52;
    
    private final Inventory inventory;
    private final String guiId;
    private final Map<Integer, GUIAction> actions;
    private final Player owner;
    private int currentPage;
    private int maxPage;
    
    /**
     * GUI动作接口
     */
    @FunctionalInterface
    public interface GUIAction {
        void execute(Player player, ClickType clickType);
    }
    
    /**
     * 点击类型枚举
     */
    public enum ClickType {
        LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE, UNKNOWN
    }
    
    /**
     * GUI持有者类
     */
    public static class ChestGUIHolder implements InventoryHolder {
        private final ChestGUI gui;
        
        public ChestGUIHolder(ChestGUI gui) {
            this.gui = gui;
        }
        
        public ChestGUI getGUI() {
            return gui;
        }
        
        @Override
        public Inventory getInventory() {
            return gui.getInventory();
        }
    }
    
    /**
     * 创建箱子GUI
     * @param owner 拥有者玩家
     * @param title GUI标题
     * @param size GUI大小（必须是9的倍数）
     * @param guiId GUI唯一标识
     */
    public ChestGUI(Player owner, String title, int size, String guiId) {
        this.owner = owner;
        this.guiId = guiId;
        this.actions = new HashMap<>();
        this.currentPage = 1;
        this.maxPage = 1;
        
        // 确保大小是9的倍数
        if (size % 9 != 0) {
            size = ((size / 9) + 1) * 9;
        }
        
        this.inventory = Bukkit.createInventory(new ChestGUIHolder(this), size, title);
    }
    
    /**
     * 设置物品到指定槽位
     * @param slot 槽位
     * @param item 物品
     * @param action 点击动作
     */
    public void setItem(int slot, ItemStack item, GUIAction action) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
            if (action != null) {
                actions.put(slot, action);
            }
        }
    }
    
    /**
     * 设置物品到指定槽位（无动作）
     * @param slot 槽位
     * @param item 物品
     */
    public void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }
    
    /**
     * 设置装饰性物品（玻璃板）
     * @param slots 槽位数组
     * @param material 材质
     * @param name 显示名称
     * @param color 颜色代码
     */
    public void setDecoration(int[] slots, Material material, String name, String color) {
        ItemStack item = createItem(material, name, null, color);
        for (int slot : slots) {
            setItem(slot, item, null);
        }
    }
    
    /**
     * 填充空槽位
     * @param material 填充材质
     * @param name 显示名称
     */
    public void fillEmpty(Material material, String name) {
        ItemStack filler = createItem(material, name, null, "§8");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                setItem(i, filler, null);
            }
        }
    }
    
    /**
     * 执行槽位动作
     * @param slot 槽位
     * @param player 玩家
     * @param clickType 点击类型
     */
    public void executeAction(int slot, Player player, ClickType clickType) {
        GUIAction action = actions.get(slot);
        if (action != null) {
            action.execute(player, clickType);
        }
    }
    
    /**
     * 打开GUI给玩家
     */
    public void open() {
        owner.openInventory(inventory);
    }
    
    /**
     * 关闭GUI
     */
    public void close() {
        owner.closeInventory();
    }
    
    /**
     * 获取GUI ID
     */
    public String getGuiId() {
        return guiId;
    }
    
    /**
     * 获取物品栏
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 获取拥有者
     */
    public Player getOwner() {
        return owner;
    }
    
    /**
     * 获取当前页码
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 设置当前页码
     */
    public void setCurrentPage(int page) {
        this.currentPage = Math.max(1, Math.min(page, maxPage));
    }
    
    /**
     * 获取最大页码
     */
    public int getMaxPage() {
        return maxPage;
    }
    
    /**
     * 设置最大页码
     */
    public void setMaxPage(int maxPage) {
        this.maxPage = Math.max(1, maxPage);
    }
    
    /**
     * 添加导航栏
     * @param hasPrevPage 是否有上一页
     * @param hasNextPage 是否有下一页
     * @param backAction 返回动作
     * @param prevAction 上一页动作
     * @param nextAction 下一页动作
     */
    public void addNavigationBar(boolean hasPrevPage, boolean hasNextPage, 
                                  GUIAction backAction, GUIAction prevAction, GUIAction nextAction) {
        // 填充导航栏背景
        ItemStack navBg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null, "§8");
        for (int slot : NAVIGATION_SLOTS) {
            setItem(slot, navBg, null);
        }
        
        // 返回按钮
        if (backAction != null) {
            ItemStack backBtn = createItem(Material.BARRIER, "§c§l返回喵~", 
                List.of("§7呜...点击返回上一级菜单的说~"), "§c");
            setItem(BACK_BUTTON_SLOT, backBtn, backAction);
        }
        
        // 上一页按钮
        if (hasPrevPage && prevAction != null) {
            ItemStack prevBtn = createItem(Material.ARROW, "§a§l上一页喵~", 
                List.of("§7点击回到上一页的说~", "§7当前: §e" + currentPage + "§7/§e" + maxPage), "§a");
            setItem(PREV_PAGE_SLOT, prevBtn, prevAction);
        }
        
        // 下一页按钮
        if (hasNextPage && nextAction != null) {
            ItemStack nextBtn = createItem(Material.ARROW, "§a§l下一页喵~", 
                List.of("§7点击翻到下一页的说~", "§7当前: §e" + currentPage + "§7/§e" + maxPage), "§a");
            setItem(NEXT_PAGE_SLOT, nextBtn, nextAction);
        }
        
        // 信息按钮
        ItemStack infoBtn = createItem(Material.BOOK, "§e§l信息喵~", 
            List.of("§7当前页码: §e" + currentPage + "§7/§e" + maxPage), "§e");
        setItem(INFO_SLOT, infoBtn, null);
    }
    
    /**
     * 创建物品
     * @param material 材质
     * @param name 显示名称
     * @param lore 描述
     * @param color 颜色代码前缀
     * @return 物品
     */
    public static ItemStack createItem(Material material, String name, List<String> lore, String color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * 创建分类图标
     * @param material 材质
     * @param name 分类名称
     * @param description 分类描述
     * @param color 颜色代码
     * @param action 点击动作
     * @return 分类物品
     */
    public static ItemStack createCategoryIcon(Material material, String name, String description, 
                                                String color, boolean hasPermission) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        if (hasPermission) {
            lore.add("§a点击打开");
        } else {
            lore.add("§c你没有权限访问");
        }
        
        return createItem(material, color + name, lore, color);
    }
    
    /**
     * 创建功能按钮
     * @param material 材质
     * @param name 按钮名称
     * @param description 功能描述
     * @param color 颜色代码
     * @return 按钮物品
     */
    public static ItemStack createButton(Material material, String name, String description, String color) {
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        lore.add("§a点击执行");
        return createItem(material, color + name, lore, color);
    }
    
    /**
     * 将Bukkit点击类型转换为GUI点击类型
     * @param eventClickType Bukkit点击类型
     * @return GUI点击类型
     */
    public static ClickType convertClickType(org.bukkit.event.inventory.ClickType eventClickType) {
        return switch (eventClickType) {
            case LEFT -> ClickType.LEFT;
            case RIGHT -> ClickType.RIGHT;
            case SHIFT_LEFT -> ClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> ClickType.SHIFT_RIGHT;
            case MIDDLE -> ClickType.MIDDLE;
            default -> ClickType.UNKNOWN;
        };
    }
}

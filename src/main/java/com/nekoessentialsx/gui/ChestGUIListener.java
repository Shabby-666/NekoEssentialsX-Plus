package com.nekoessentialsx.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 箱子GUI事件监听器
 * 处理所有GUI相关的事件
 */
public class ChestGUIListener implements Listener {
    
    private final ChestGUIManager guiManager;
    
    public ChestGUIListener(ChestGUIManager guiManager) {
        this.guiManager = guiManager;
    }
    
    /**
     * 处理物品栏点击事件
     * @param event 点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是箱子GUI
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }
        
        InventoryHolder holder = clickedInventory.getHolder();
        if (!(holder instanceof ChestGUI.ChestGUIHolder)) {
            return;
        }
        
        // 取消事件，防止玩家取出物品
        event.setCancelled(true);
        
        // 获取玩家和GUI
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ChestGUI.ChestGUIHolder guiHolder = (ChestGUI.ChestGUIHolder) holder;
        ChestGUI gui = guiHolder.getGUI();
        
        // 检查是否是该玩家的GUI
        if (!player.getUniqueId().equals(gui.getOwner().getUniqueId())) {
            return;
        }
        
        // 获取点击的槽位
        int slot = event.getRawSlot();
        
        // 检查槽位是否有效
        if (slot < 0 || slot >= clickedInventory.getSize()) {
            return;
        }
        
        // 转换点击类型
        ChestGUI.ClickType clickType = ChestGUI.convertClickType(event.getClick());
        
        // 执行槽位动作
        gui.executeAction(slot, player, clickType);
        
        // 播放点击音效（视觉反馈）
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * 处理物品栏打开事件
     * @param event 打开事件
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ChestGUI.ChestGUIHolder) {
            // 可以在这里添加打开GUI时的逻辑
        }
    }
    
    /**
     * 处理物品栏关闭事件
     * @param event 关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof ChestGUI.ChestGUIHolder) {
            ChestGUI.ChestGUIHolder guiHolder = (ChestGUI.ChestGUIHolder) holder;
            ChestGUI gui = guiHolder.getGUI();
            
            // 检查是否是该玩家的GUI
            if (player.getUniqueId().equals(gui.getOwner().getUniqueId())) {
                // 延迟移除GUI记录，防止在点击处理完成前移除
                org.bukkit.Bukkit.getScheduler().runTaskLater(
                    guiManager.getPlugin(),
                    () -> guiManager.removePlayerGUI(player),
                    1L
                );
            }
        }
    }
}

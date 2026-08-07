package com.nekoessentialsx.gui;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    private final GUIManager guiManager;
    private final NekoEssentialX plugin;

    public GUIListener(GUIManager guiManager) {
        this.guiManager = guiManager;
        this.plugin = NekoEssentialX.getPlugin(NekoEssentialX.class);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GUIManager.GUIHolder) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }
            
            String localizedName = guiManager.getButtonData(clickedItem);
            if (localizedName == null) {
                return;
            }
            
            GUIManager.GUISession session = guiManager.getOrCreateSession(player);
            
            try {
                // 处理导航按钮点击
                GUIManager.GUIItemType itemType = GUIManager.GUIItemType.valueOf(localizedName);
                handleNavigationClick(player, session, itemType);
            } catch (IllegalArgumentException e) {
                // 如果不是导航按钮，可能是头衔ID
                handleTitleClick(event, player, session, localizedName);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof GUIManager.GUIHolder) {
            plugin.getLogger().info(event.getPlayer().getName() + " opened a GUI");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GUIManager.GUIHolder) {
            plugin.getLogger().info(event.getPlayer().getName() + " closed a GUI");
            // 移除会话清理逻辑，避免点击物品时会话被重置
            // guiManager.clearSession((Player) event.getPlayer());
        }
    }
    
    /**
     * 处理导航按钮点击
     */
    private void handleNavigationClick(Player player, GUIManager.GUISession session, GUIManager.GUIItemType itemType) {
        switch (itemType) {
            case TITLE_SHOP_ITEM:
                session.setCurrentType(GUIManager.GUIType.TITLE_SHOP);
                guiManager.openTitleShop(player, 1);
                break;
            case PERSONAL_TITLE_ITEM:
                session.setCurrentType(GUIManager.GUIType.PERSONAL_TITLES);
                guiManager.openPersonalTitles(player, 1);
                break;
            case PAGE_NEXT:
                int nextPage = session.getCurrentPage() + 1;
                session.setCurrentPage(nextPage);
                handlePageChange(player, session);
                break;
            case PAGE_PREV:
                int prevPage = session.getCurrentPage() - 1;
                if (prevPage >= 1) {
                    session.setCurrentPage(prevPage);
                    handlePageChange(player, session);
                }
                break;
            case CUSTOM_TITLE_CREATE:
                // 关闭GUI并提示玩家输入自定义头衔信息
                player.closeInventory();
                player.sendMessage("§a请在聊天框中输入你要创建的自定义头衔的说~喵~");
                player.sendMessage("§7格式：<id> <name>");
                player.sendMessage("§7示例：sakura 樱花喵");
                player.sendMessage("§7名称字符限制：1-20个字符的说~");
                player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                // 设置玩家输入状态
                guiManager.getChatInputListener().setPlayerInputState(player, ChatInputListener.InputState.WAITING_FOR_TITLE_NAME);
                break;
            case ADMIN_TITLE_MANAGER:
                // 从主菜单进入管理员头衔管理界面
                session.setCurrentType(GUIManager.GUIType.ADMIN_TITLE_MANAGER);
                guiManager.openAdminTitleManager(player, 1);
                break;
            case ADMIN_CREATE_TITLE:
                // 检查当前是否在管理员头衔管理界面
                if (session.getCurrentType() == GUIManager.GUIType.ADMIN_TITLE_MANAGER) {
                    // 在管理员界面点击创建新头衔，关闭GUI并提示输入系统头衔信息
                    player.closeInventory();
                    player.sendMessage("§a请在聊天框中输入系统头衔的ID、名称和前缀的说~喵~");
                    player.sendMessage("§7格式：<id> <name> <prefix>");
                    player.sendMessage("§7示例：vip 会员 [VIP] ");
                    player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                    // 设置玩家输入状态为等待管理员头衔信息
                    guiManager.getChatInputListener().setPlayerInputState(player, ChatInputListener.InputState.WAITING_FOR_ADMIN_TITLE_INFO);
                }
                break;
            case BACK_TO_MAIN:
                // 返回主菜单
                session.setCurrentType(GUIManager.GUIType.MAIN);
                session.setCurrentPage(1);
                guiManager.openMainGUI(player);
                break;
            case CONFIRM_CREATE:
                // 确认创建自定义头衔
                guiManager.handleConfirmCreateTitle(player);
                break;
            case CANCEL_CREATE:
                // 取消创建自定义头衔
                guiManager.handleCancelCreateTitle(player);
                break;
            case TP_MENU_ITEM:
                // 打开传送系统菜单
                session.setCurrentType(GUIManager.GUIType.TP_MENU);
                guiManager.openTPMenu(player);
                break;
            case ECONOMY_MENU_ITEM:
                // 打开经济系统菜单
                session.setCurrentType(GUIManager.GUIType.ECONOMY_MENU);
                guiManager.openEconomyMenu(player);
                break;
            case PLUGIN_MENU_ITEM:
                // 打开插件管理菜单
                session.setCurrentType(GUIManager.GUIType.PLUGIN_MENU);
                guiManager.openPluginMenu(player);
                break;
            case TP_SEND_REQUEST:
                // 发送传送请求
                player.closeInventory();
                player.sendMessage("§a请在聊天框中输入要发送传送请求的玩家名称的说~喵~");
                player.sendMessage("§7格式：<player>");
                player.sendMessage("§7示例：Steve");
                player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                // 这里可以添加聊天输入状态处理
                player.sendMessage("§e发送传送请求功能正在开发中，暂时请使用命令：/tpa <player> 的说~喵~");
                guiManager.openTPMenu(player);
                break;
            case TP_REQUESTS_ITEM:
                // 查看传送请求
                guiManager.openTPRequestsGUI(player);
                break;
            case TP_CANCEL_REQUEST:
                // 取消传送请求
                player.closeInventory();
                player.sendMessage("§a请在聊天框中输入要取消传送请求的玩家名称（留空取消所有）的说~喵~");
                player.sendMessage("§7格式：[player]");
                player.sendMessage("§7示例：Steve");
                player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                // 这里可以添加聊天输入状态处理
                player.sendMessage("§e取消传送请求功能正在开发中，暂时请使用命令：/tpacancel [player] 的说~喵~");
                guiManager.openTPMenu(player);
                break;
            case ECONOMY_VIEW_BALANCE:
                // 查看余额
                double balance = guiManager.getPlugin().getEconomyManager().getBalance(player);
                player.sendMessage("§a你的当前余额是：§6" + guiManager.getPlugin().getEconomyManager().format(balance) + " 的说~喵~");
                guiManager.openEconomyMenu(player);
                break;
            case ECONOMY_SEND_MONEY:
                // 发送金钱
                player.closeInventory();
                player.sendMessage("§a请在聊天框中输入要发送金钱的玩家名称和金额的说~喵~");
                player.sendMessage("§7格式：<player> <amount>");
                player.sendMessage("§7示例：Steve 100");
                player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                // 这里可以添加聊天输入状态处理
                player.sendMessage("§e发送金钱功能正在开发中，暂时请使用命令：/money pay <player> <amount> 的说~喵~");
                guiManager.openEconomyMenu(player);
                break;
            case ECONOMY_ADMIN_ITEM:
                // 管理员经济管理
                guiManager.openEconomyAdminGUI(player);
                break;
            case ECONOMY_ADMIN_DEPOSIT:
                // 充值或更改货币名称，根据当前GUI类型判断
                GUIManager.GUISession ecoSession = guiManager.getOrCreateSession(player);
                if (ecoSession.getCurrentType() == GUIManager.GUIType.ECONOMY_MENU) {
                    // 在经济系统主菜单中，点击的是更改货币名称
                    player.closeInventory();
                    player.sendMessage("§a请在聊天框中输入新的货币名称的说~喵~");
                    player.sendMessage("§7字符限制：1-20个字符的说~");
                    player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                    // 设置玩家输入状态
                    guiManager.getChatInputListener().setPlayerInputState(player, ChatInputListener.InputState.WAITING_FOR_CURRENCY_NAME);
                } else if (ecoSession.getCurrentType() == GUIManager.GUIType.ECONOMY_ADMIN) {
                    // 在管理员经济管理界面，点击的是充值
                    player.closeInventory();
                    player.sendMessage("§a请在聊天框中输入玩家名称和充值金额的说~喵~");
                    player.sendMessage("§7格式：<player> <amount>");
                    player.sendMessage("§7示例：Steve 100");
                    player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                    player.sendMessage("§e管理员充值功能正在开发中，暂时请使用命令：/money give <player> <amount> 的说~喵~");
                    // 返回经济管理菜单
                    guiManager.openEconomyAdminGUI(player);
                }
                break;
            case ECONOMY_ADMIN_WITHDRAW:
                // 扣款
                player.closeInventory();
                player.sendMessage("§a请在聊天框中输入玩家名称和扣款金额的说~喵~");
                player.sendMessage("§7格式：<player> <amount>");
                player.sendMessage("§7示例：Steve 100");
                player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                player.sendMessage("§e管理员扣款功能正在开发中，暂时请使用命令：/money take <player> <amount> 的说~喵~");
                // 返回经济管理菜单
                guiManager.openEconomyAdminGUI(player);
                break;
            case PLUGIN_VERSION:
                // 查看插件版本
                player.sendMessage("§aNekoEssentialsX+ 当前版本：1.0.0-beta 的说~喵~");
                guiManager.openPluginMenu(player);
                break;
            case PLUGIN_RELOAD:
                // 重载插件配置
                guiManager.getPlugin().reloadConfig();
                player.sendMessage("§a插件配置已重载的说！喵~");
                guiManager.openPluginMenu(player);
                break;
            case NEWBIE_GIFT_ITEM:
                // 处理新手大礼包领取
                player.closeInventory();
                guiManager.getPlugin().getNewbieGiftManager().handleGiftClaim(player);
                break;
            default:
                player.sendMessage("§c呜...未知的按钮类型的说..." + itemType + " 喵~");
                break;
        }
    }
    
    /**
     * 处理头衔点击
     */
    private void handleTitleClick(InventoryClickEvent event, Player player, GUIManager.GUISession session, String titleId) {
        switch (session.getCurrentType()) {
            case MAIN:
                break;
            case TITLE_SHOP:
                guiManager.handleTitlePurchase(player, titleId);
                break;
            case PERSONAL_TITLES:
                guiManager.handleTitleEquip(player, titleId);
                break;
            case ADMIN_TITLE_MANAGER:
                // 管理员头衔管理，需要根据点击方式处理不同操作
                if (event.isRightClick()) {
                    // 右键删除
                    player.closeInventory();
                    player.sendMessage("§a已删除系统头衔的说~: §b" + titleId + " 喵~");
                    guiManager.handleAdminDeleteTitle(player, titleId);
                    // 重新打开管理员头衔管理界面，更新列表
                    guiManager.openAdminTitleManager(player, session.getCurrentPage());
                } else if (event.isShiftClick() && event.isLeftClick()) {
                    // Shift+左键切换状态
                    // 获取当前头衔的启用状态
                    boolean isEnabled = plugin.getTitleManager().getTitle(titleId) != null && plugin.getTitleManager().getTitle(titleId).isEnabled();
                    // 切换状态
                    plugin.getTitleManager().toggleTitleEnabled(titleId, !isEnabled);
                    player.sendMessage("§a已" + (!isEnabled ? "启用" : "禁用") + "系统头衔的说~: §b" + titleId + " 喵~");
                    // 重新打开管理员头衔管理界面，更新列表
                    guiManager.openAdminTitleManager(player, session.getCurrentPage());
                } else if (event.isLeftClick()) {
                    // 左键编辑，关闭GUI并提示输入新的头衔信息
                    player.closeInventory();
                    player.sendMessage("§a请在聊天框中输入系统头衔的新名称和前缀的说~喵~");
                    player.sendMessage("§7格式：<name> <prefix>");
                    player.sendMessage("§7示例：超级VIP [超级VIP] ");
                    player.sendMessage("§7输入 'cancel' 可以取消操作的说~");
                    // 设置玩家输入状态为等待管理员编辑头衔信息，并保存当前头衔ID到会话
                    session.setData("editingTitleId", titleId);
                    guiManager.getChatInputListener().setPlayerInputState(player, ChatInputListener.InputState.WAITING_FOR_ADMIN_EDIT_TITLE_INFO);
                }
                break;
            case TP_REQUESTS:
                // 处理传送请求点击
                if (titleId.startsWith("tp_request_")) {
                    // 从titleId中提取发送者名称
                    String senderName = titleId.substring("tp_request_".length());
                    if (event.isLeftClick()) {
                        // 左键接受请求
                        plugin.getTPAManager().acceptRequest(player, senderName);
                        // 重新打开传送请求GUI，更新列表
                        guiManager.openTPRequestsGUI(player);
                    } else if (event.isRightClick()) {
                        // 右键拒绝请求
                        plugin.getTPAManager().denyRequest(player, senderName);
                        // 重新打开传送请求GUI，更新列表
                        guiManager.openTPRequestsGUI(player);
                    }
                }
                break;
            default:
                player.sendMessage("§c呜...未知的界面类型的说..." + session.getCurrentType() + " 喵~");
                break;
        }
    }
    
    /**
     * 处理页面切换
     */
    private void handlePageChange(Player player, GUIManager.GUISession session) {
        switch (session.getCurrentType()) {
            case MAIN:
                break;
            case TITLE_SHOP:
                guiManager.openTitleShop(player, session.getCurrentPage());
                break;
            case PERSONAL_TITLES:
                guiManager.openPersonalTitles(player, session.getCurrentPage());
                break;
            case ADMIN_TITLE_MANAGER:
                guiManager.openAdminTitleManager(player, session.getCurrentPage());
                break;
            default:
                player.sendMessage("§c呜...这个界面不支持分页的说..." + session.getCurrentType() + " 喵~");
                break;
        }
    }
}
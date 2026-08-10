package com.nekoessentialsx.gui;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.economy.EconomyManager;
import com.nekoessentialsx.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class ChatInputListener implements Listener {
    private final NekoEssentialX plugin;
    private final GUIManager guiManager;
    private final TitleManager titleManager;
    private final EconomyManager economyManager;
    private final Map<Player, InputState> playerInputStates = new HashMap<>();

    public enum InputState {
        WAITING_FOR_TITLE_NAME,
        WAITING_FOR_ADMIN_TITLE_INFO,
        WAITING_FOR_ADMIN_EDIT_TITLE_INFO,
        WAITING_FOR_CURRENCY_NAME,
        WAITING_FOR_ECONOMY_AMOUNT
    }

    private final Map<String, String> pendingEconomyOps = new HashMap<>();

    public ChatInputListener(NekoEssentialX plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.titleManager = plugin.getTitleManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (playerInputStates.containsKey(player)) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            handleInput(player, message);
        }
    }

    private void handleInput(Player player, String input) {
        InputState state = playerInputStates.remove(player);
        if (state == null) return;

        switch (state) {
            case WAITING_FOR_TITLE_NAME:
                handleTitleNameInput(player, input);
                break;
            case WAITING_FOR_ADMIN_TITLE_INFO:
                handleAdminTitleInfoInput(player, input);
                break;
            case WAITING_FOR_ADMIN_EDIT_TITLE_INFO:
                handleAdminEditTitleInfoInput(player, input);
                break;
            case WAITING_FOR_CURRENCY_NAME:
                handleCurrencyNameInput(player, input);
                break;
            case WAITING_FOR_ECONOMY_AMOUNT:
                handleEconomyAmountInput(player, input);
                break;
        }
    }

    private void handleTitleNameInput(Player player, String input) {
        // 检查是否取消操作
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "创建头衔操作已取消！");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getChestGUIManager().openMainMenu(player);
            });
            return;
        }

        // 解析输入，格式：<id> <name>
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 2) {
            player.sendMessage(ChatColor.RED + "输入格式错误！请使用以下格式：");
            player.sendMessage(ChatColor.YELLOW + "<id> <name>");
            player.sendMessage(ChatColor.YELLOW + "示例：sakura 樱花喵");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            setPlayerInputState(player, InputState.WAITING_FOR_TITLE_NAME);
            return;
        }

        String titleId = parts[0].trim();
        String titleName = parts[1].trim();

        // 验证输入
        if (titleId.isEmpty() || titleName.isEmpty()) {
            player.sendMessage(ChatColor.RED + "头衔ID和名称都不能为空！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            setPlayerInputState(player, InputState.WAITING_FOR_TITLE_NAME);
            return;
        }

        if (titleName.length() > 20) {
            player.sendMessage(ChatColor.RED + "头衔名称不能超过20个字符！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            setPlayerInputState(player, InputState.WAITING_FOR_TITLE_NAME);
            return;
        }

        // 校验头衔ID是否已存在
        if (titleManager.getTitle(titleId) != null || plugin.getGuiManager().isCustomTitleIdTaken(player, titleId)) {
            player.sendMessage(ChatColor.RED + "头衔ID已存在，请换一个的说！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            setPlayerInputState(player, InputState.WAITING_FOR_TITLE_NAME);
            return;
        }

        // 保存当前输入到会话，供确认GUI使用
        GUIManager.GUISession session = plugin.getGuiManager().getOrCreateSession(player);
        session.setData("titleId", titleId);
        session.setData("titleName", titleName);

        // 直接打开新版确认箱子GUI（无需再输入 confirm）
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getChestGUIManager().openConfirmCreateCustomTitleGUI(player, titleId, titleName);
        });
    }

    private void handleAdminTitleInfoInput(Player player, String input) {
        // 检查是否取消操作
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "创建系统头衔操作已取消！");
            return;
        }

        // 解析输入，格式：<id> <name> <prefix>
        String[] parts = input.split("\s+", 3);
        if (parts.length < 3) {
            player.sendMessage(ChatColor.RED + "输入格式错误！请使用以下格式：");
            player.sendMessage(ChatColor.YELLOW + "<id> <name> <prefix>");
            player.sendMessage(ChatColor.YELLOW + "示例：vip 会员 [VIP] ");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_TITLE_INFO);
            return;
        }

        String titleId = parts[0];
        String titleName = parts[1];
        String prefix = parts[2];

        // 验证输入
        if (titleId.isEmpty() || titleName.isEmpty() || prefix.isEmpty()) {
            player.sendMessage(ChatColor.RED + "所有字段都不能为空！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_TITLE_INFO);
            return;
        }

        if (titleName.length() > 20) {
            player.sendMessage(ChatColor.RED + "头衔名称不能超过20个字符！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_TITLE_INFO);
            return;
        }

        if (prefix.length() > 30) {
            player.sendMessage(ChatColor.RED + "头衔前缀不能超过30个字符！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_TITLE_INFO);
            return;
        }

        // 调用TitleManager创建系统头衔
        titleManager.createTitle(titleId, titleName, prefix, "", "nekoessentialsx.titles." + titleId, 1, true);

        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "系统头衔创建成功！");
        player.sendMessage(ChatColor.GREEN + "头衔ID: " + ChatColor.AQUA + titleId);
        player.sendMessage(ChatColor.GREEN + "头衔名称: " + ChatColor.AQUA + titleName);
        player.sendMessage(ChatColor.GREEN + "头衔前缀: " + ChatColor.AQUA + prefix);

        // 使用主线程打开GUI，避免异步线程调用同步方法
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getChestGUIManager().openTitleAdminMenu(player, 1);
        });
    }

    private void handleAdminEditTitleInfoInput(Player player, String input) {
        // 检查是否取消操作
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "编辑系统头衔操作已取消！");
            return;
        }

        // 解析输入，格式：<name> <prefix>
        String[] parts = input.split("\s+", 2);
        if (parts.length < 2) {
            player.sendMessage(ChatColor.RED + "输入格式错误！请使用以下格式：");
            player.sendMessage(ChatColor.YELLOW + "<name> <prefix>");
            player.sendMessage(ChatColor.YELLOW + "示例：超级VIP [超级VIP] ");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_EDIT_TITLE_INFO);
            return;
        }

        // 获取正在编辑的头衔ID
        GUIManager.GUISession session = guiManager.getOrCreateSession(player);
        String titleId = (String) session.getData().get("editingTitleId");
        if (titleId == null) {
            player.sendMessage(ChatColor.RED + "无法获取正在编辑的头衔ID！");
            return;
        }

        String titleName = parts[0];
        String prefix = parts[1];

        // 验证输入
        if (titleName.isEmpty() || prefix.isEmpty()) {
            player.sendMessage(ChatColor.RED + "所有字段都不能为空！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_EDIT_TITLE_INFO);
            return;
        }

        if (titleName.length() > 20) {
            player.sendMessage(ChatColor.RED + "头衔名称不能超过20个字符！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_EDIT_TITLE_INFO);
            return;
        }

        if (prefix.length() > 30) {
            player.sendMessage(ChatColor.RED + "头衔前缀不能超过30个字符！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            // 重新设置输入状态，让玩家继续输入
            playerInputStates.put(player, InputState.WAITING_FOR_ADMIN_EDIT_TITLE_INFO);
            return;
        }

        // 调用TitleManager编辑系统头衔
        titleManager.editTitle(titleId, titleName, prefix, "", "nekoessentialsx.titles." + titleId, 1, true);

        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "系统头衔编辑成功！");
        player.sendMessage(ChatColor.GREEN + "头衔ID: " + ChatColor.AQUA + titleId);
        player.sendMessage(ChatColor.GREEN + "头衔名称: " + ChatColor.AQUA + titleName);
        player.sendMessage(ChatColor.GREEN + "头衔前缀: " + ChatColor.AQUA + prefix);

        // 使用主线程打开GUI，避免异步线程调用同步方法
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getChestGUIManager().openTitleAdminMenu(player, 1);
        });
    }

    public void setPlayerInputState(Player player, InputState state) {
        playerInputStates.put(player, state);
    }

    private void handleCurrencyNameInput(Player player, String currencyName) {
        // 检查是否取消操作
        if (currencyName.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "更改货币名称操作已取消！");
            return;
        }

        // 验证货币名称长度
        if (currencyName.length() < 1) {
            player.sendMessage(ChatColor.RED + "货币名称不能为空！");
            return;
        }

        if (currencyName.length() > 20) {
            player.sendMessage(ChatColor.RED + "货币名称不能超过20个字符！");
            return;
        }

        // 更新配置文件中的货币名称
        plugin.getConfig().set("economy.currency-name", currencyName);
        plugin.getConfig().set("economy.currency-name-plural", currencyName);
        plugin.saveConfig();
        
        // 更新EconomyManager中的货币名称
        plugin.getEconomyManager().reloadCurrencyName();

        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "货币名称更改成功！");
        player.sendMessage(ChatColor.GREEN + "新的货币名称：" + ChatColor.AQUA + currencyName);

        // 使用主线程打开GUI，避免异步线程调用同步方法
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getChestGUIManager().openEconomyMenu(player);
        });
    }

    public void clearPlayerInputState(Player player) {
        playerInputStates.remove(player);
        pendingEconomyOps.remove(player.getName().toLowerCase());
    }

    public boolean hasPlayerInputState(Player player) {
        return playerInputStates.containsKey(player);
    }

    public void setPendingEconomyOperation(Player player, String opType, String targetName) {
        pendingEconomyOps.put(player.getName().toLowerCase(), opType + ":" + targetName.toLowerCase());
        playerInputStates.put(player, InputState.WAITING_FOR_ECONOMY_AMOUNT);
    }

    private void handleEconomyAmountInput(Player player, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "操作已取消！");
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getChestGUIManager().openEconomyMenu(player));
            return;
        }

        String opKey = pendingEconomyOps.remove(player.getName().toLowerCase());
        if (opKey == null) {
            player.sendMessage(ChatColor.RED + "操作状态丢失，请重试！");
            return;
        }

        String[] parts = opKey.split(":", 2);
        String opType = parts[0];
        String targetName = parts[1];

        double amount;
        try {
            amount = Double.parseDouble(input.trim());
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "请输入有效的正数金额！");
            player.sendMessage(ChatColor.YELLOW + "输入 'cancel' 取消操作");
            pendingEconomyOps.put(player.getName().toLowerCase(), opKey);
            playerInputStates.put(player, InputState.WAITING_FOR_ECONOMY_AMOUNT);
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家 " + targetName + " 不在线！");
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getChestGUIManager().openEconomyMenu(player));
            return;
        }

        boolean success = false;
        switch (opType) {
            case "deposit": {
                success = economyManager.depositPlayer(target, amount);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已为 " + ChatColor.AQUA + target.getName()
                            + ChatColor.GREEN + " 充值 " + ChatColor.GOLD + economyManager.format(amount));
                    target.sendMessage(ChatColor.GREEN + "管理员为你充值了 " + ChatColor.GOLD + economyManager.format(amount));
                }
                break;
            }
            case "withdraw": {
                success = economyManager.withdrawPlayer(target, amount);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已从 " + ChatColor.AQUA + target.getName()
                            + ChatColor.GREEN + " 扣款 " + ChatColor.GOLD + economyManager.format(amount));
                    target.sendMessage(ChatColor.RED + "管理员从你的账户扣款 " + ChatColor.GOLD + economyManager.format(amount));
                }
                break;
            }
            case "setbalance": {
                double current = economyManager.getBalance(target);
                double diff = amount - current;
                if (diff > 0) {
                    success = economyManager.depositPlayer(target, diff);
                } else if (diff < 0) {
                    success = economyManager.withdrawPlayer(target, -diff);
                } else {
                    success = true;
                }
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已将 " + ChatColor.AQUA + target.getName()
                            + ChatColor.GREEN + " 的余额设置为 " + ChatColor.GOLD + economyManager.format(amount));
                }
                break;
            }
            case "transfer": {
                success = economyManager.transfer(player, target, amount);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "已向 " + ChatColor.AQUA + target.getName()
                            + ChatColor.GREEN + " 转账 " + ChatColor.GOLD + economyManager.format(amount));
                    target.sendMessage(ChatColor.GREEN + "收到来自 " + ChatColor.AQUA + player.getName()
                            + ChatColor.GREEN + " 的转账 " + ChatColor.GOLD + economyManager.format(amount));
                }
                break;
            }
        }

        if (!success) {
            player.sendMessage(ChatColor.RED + "操作失败！可能余额不足或参数错误。");
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getChestGUIManager().openEconomyMenu(player));
    }
}
package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandmoney implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final EconomyManager economyManager;

    public Commandmoney(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        // 注册Tab补全器到money命令及其所有别名
        plugin.getCommand("money").setTabCompleter(this);
        plugin.getCommand("eco").setTabCompleter(this);
        plugin.getCommand("bal").setTabCompleter(this);
        plugin.getCommand("cash").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!economyManager.isEnabled()) {
                String message = "§c呜...经济系统已经被主人关掉啦，暂时用不了的说~喵~";
                if (sender instanceof Player) {
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, message);
                        return true;
                    }
                }
                sender.sendMessage(message);
                return true;
            }

            if (args.length < 1) {
                // 显示余额
                checkBalance(sender);
                return true;
            }

            final String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "balance":
                case "bal":
                case "me":
                    checkBalance(sender);
                    break;
                case "pay":
                    payPlayer(sender, args);
                    break;
                case "deposit":
                    deposit(sender, args);
                    break;
                case "withdraw":
                    withdraw(sender, args);
                    break;
                case "give":
                    giveMoney(sender, args);
                    break;
                case "take":
                    takeMoney(sender, args);
                    break;
                case "name":
                    setCurrencyName(sender, args);
                    break;
                case "help":
                    showHelp(sender, label);
                    break;
                default:
                    sender.sendMessage("§c呜...这个子命令我不认识的说~ §e" + args[0] + "§c 喵~");
                    sender.sendMessage("§c想看看有哪些命令的话，就用 §e/" + label + " help §c吧喵~");
                    break;
            }
        } catch (Exception e) {
            sender.sendMessage("§c呜哇...执行命令的时候出错啦！米娜对不起了喵~");
            plugin.getLogger().severe("执行money命令时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        try {
            if (args.length == 1) {
                // 补全子命令
                String[] subCommands = {"balance", "bal", "me", "pay", "deposit", "withdraw", "give", "take", "name", "help"};
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length >= 2) {
                String subCommand = args[0].toLowerCase();

                switch (subCommand) {
                    case "pay":
                    case "give":
                    case "take":
                        // 补全玩家名与选择器
                        if (args.length == 2) {
                            completions = com.nekoessentialsx.util.PlayerSelector.complete(sender, args[1]);
                        }
                        break;
                    case "deposit":
                    case "withdraw":
                        // 补全玩家名与选择器，同时给出金额示例
                        if (args.length == 2) {
                            completions = com.nekoessentialsx.util.PlayerSelector.complete(sender, args[1]);
                            completions.add("100");
                            completions.add("1000");
                        }
                        break;
                    case "name":
                        // 补全货币名称示例
                        if (args.length == 2) {
                            completions.add("金币");
                            completions.add("点券");
                            completions.add("钻石");
                            completions.add("硬币");
                        }
                        break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Tab补全时发生错误: " + e.getMessage());
        }

        Collections.sort(completions);
        return completions;
    }

    /**
     * 显示余额
     */
    private void checkBalance(CommandSender sender) {
        Player target;
        if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c呜...主控台是看不到余额的说~只有玩家才能看的喵~");
            return;
        }

        double balance = economyManager.getBalance(target);
        String message = "§a你的余额是: §6" + economyManager.format(balance) + "§a 的说~要好好攒钱买小鱼干哦喵~";

        if (sender instanceof Player) {
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
                return;
            }
        }

        sender.sendMessage(message);
    }

    /**
     * 支付给其他玩家
     */
    private void payPlayer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c呜...只有玩家才可以用这个命令的说~主人没有这个权限的喵~");
            return;
        }

        if (args.length < 3) {
            String message = "§c呜...正确的用法是这样哦: /money pay <玩家|@选择器> <金额> 喵~";
            Player player = (Player) sender;
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        Player payer = (Player) sender;
        java.util.List<Player> recipients = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);

        if (recipients.isEmpty()) {
            String message = "§c呜...找不到这个玩家的说~喵~";
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage(payer, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            String message = "§c呜...要输入一个有效的金额才行哦~喵~";
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage(payer, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        if (amount <= 0) {
            String message = "§c呜...金额要比0大才可以哦，太小了我可不收的喵~";
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage(payer, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        boolean anySuccess = false;
        for (Player recipient : recipients) {
            if (economyManager.hasBalance(payer, amount)) {
                if (economyManager.transfer(payer, recipient, amount)) {
                    anySuccess = true;
                    String payerMessage = "§a呜呼~成功把 §e" + economyManager.format(amount) + " §a 给了 §e" + recipient.getName() + "§a 啦喵~";
                    String recipientMessage = "§a收到一份来自 §e" + payer.getName() + " §a 的 §e" + economyManager.format(amount) + "§a！惊喜的说~喵~";

                    CatChatProcessor processor2 = CatChatProcessor.getInstance();
                    if (processor2 != null) {
                        processor2.sendCatStyleMessage(payer, payerMessage);
                        processor2.sendCatStyleMessage(recipient, recipientMessage);
                    } else {
                        payer.sendMessage(payerMessage);
                        recipient.sendMessage(recipientMessage);
                    }
                } else {
                    String message = "§c呜...支付失败了的说~呜呜喵~";
                    CatChatProcessor processor2 = CatChatProcessor.getInstance();
                    if (processor2 != null) {
                        processor2.sendCatStyleMessage(payer, message);
                    } else {
                        sender.sendMessage(message);
                    }
                }
            } else {
                String message = "§c呜...你的余额不够用的说~先去赚点小鱼干再来吧喵~";
                CatChatProcessor processor2 = CatChatProcessor.getInstance();
                if (processor2 != null) {
                    processor2.sendCatStyleMessage(payer, message);
                } else {
                    sender.sendMessage(message);
                }
            }
        }
        if (!anySuccess) {
            String message = "§c呜...支付失败啦，一单都没有转出去的说~喵~";
            CatChatProcessor processor2 = CatChatProcessor.getInstance();
            if (processor2 != null) {
                processor2.sendCatStyleMessage(payer, message);
            } else {
                sender.sendMessage(message);
            }
        }
    }

    /**
     * 充值（管理员）
     */
    private void deposit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.economy.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        if (args.length < 3) {
            String message = "§c呜...正确的用法是这样哦: /money deposit <玩家|@选择器> <金额> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);
        if (targets.isEmpty()) {
            String message = "§c呜...找不到这个玩家的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            String message = "§c呜...要输入一个有效的金额才行哦~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        if (amount <= 0) {
            String message = "§c呜...金额要比0大才可以哦~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        for (Player target : targets) {
            if (economyManager.depositPlayer(target, amount)) {
                String senderMessage = "§a呜呼~成功给 §e" + target.getName() + " §a 充了 §e" + economyManager.format(amount) + " §a 的说~喵~";
                String targetMessage = "§a收到一份 §e" + economyManager.format(amount) + " §a 的充值！主人对你好好的说~喵~";

                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, senderMessage);
                        processor.sendCatStyleMessage(target, targetMessage);
                        continue;
                    }
                }

                sender.sendMessage(senderMessage);
                target.sendMessage(targetMessage);
            } else {
                String message = "§c呜...充值失败了的说~喵~";
                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, message);
                        continue;
                    }
                }
                sender.sendMessage(message);
            }
        }
    }

    /**
     * 提款（管理员）
     */
    private void withdraw(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.economy.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        if (args.length < 3) {
            String message = "§c呜...正确的用法是这样哦: /money withdraw <玩家|@选择器> <金额> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);
        if (targets.isEmpty()) {
            String message = "§c呜...找不到这个玩家的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            String message = "§c呜...要输入一个有效的金额才行哦~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        if (amount <= 0) {
            String message = "§c呜...金额要比0大才可以哦~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        for (Player target : targets) {
            if (economyManager.withdrawPlayer(target, amount)) {
                String senderMessage = "§a呜呼~成功从 §e" + target.getName() + " §a 那里拿走了 §e" + economyManager.format(amount) + " §a 的说~喵~";
                String targetMessage = "§c呜...主人从你的账户里拿走了 §e" + economyManager.format(amount) + " §c ...不要太难过的说喵~";

                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, senderMessage);
                        processor.sendCatStyleMessage(target, targetMessage);
                        continue;
                    }
                }

                sender.sendMessage(senderMessage);
                target.sendMessage(targetMessage);
            } else {
                String message = "§c呜...提款失败了的说~喵~";
                if (sender instanceof Player) {
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        processor.sendCatStyleMessage((Player) sender, message);
                        continue;
                    }
                }
                sender.sendMessage(message);
            }
        }
    }

    /**
     * 给予玩家金钱（管理员）
     */
    private void giveMoney(CommandSender sender, String[] args) {
        deposit(sender, args);
    }

    /**
     * 从玩家扣除金钱（管理员）
     */
    private void takeMoney(CommandSender sender, String[] args) {
        withdraw(sender, args);
    }

    /**
     * 显示帮助信息
     */
    /**
     * 设置货币名称（管理员）
     */
    private void setCurrencyName(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.economy.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        if (args.length < 2) {
            String message = "§c呜...正确的用法是这样哦: /money name <货币名称> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                    return;
                }
            }
            sender.sendMessage(message);
            return;
        }

        // 获取新的货币名称
        String newCurrencyName = args[1];
        
        // 更新配置文件
        plugin.getConfig().set("economy.currency-name", newCurrencyName);
        plugin.getConfig().set("economy.currency-name-plural", newCurrencyName);
        plugin.saveConfig();
        
        // 发送成功消息
        String message = "§a呜呼~成功把货币的名称改成了: §b" + newCurrencyName + "§a 的说~要记住哦喵~";
        if (sender instanceof Player) {
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
                return;
            }
        }
        sender.sendMessage(message);
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(CommandSender sender, String label) {
        String[] messages = {
            "§6===== §e经济系统帮助 §6=====",
            "§e/" + label + " §6- 查看自己的余额",
            "§e/" + label + " balance §6- 查看自己的余额",
            "§e/" + label + " pay <玩家|@选择器> <金额> §6- 支付给其他玩家",
            "§e/" + label + " deposit <玩家|@选择器> <金额> §6- 给玩家充值（管理员）",
            "§e/" + label + " withdraw <玩家|@选择器> <金额> §6- 从玩家账户扣款（管理员）",
            "§e/" + label + " give <玩家|@选择器> <金额> §6- 给玩家充值（管理员）",
            "§e/" + label + " take <玩家|@选择器> <金额> §6- 从玩家账户扣款（管理员）",
            "§e/" + label + " name <货币名称> §6- 设置货币名称（管理员）"
        };

        if (sender instanceof Player) {
            Player player = (Player) sender;
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                for (String message : messages) {
                    processor.sendCatStyleMessage(player, message);
                }
                return;
            }
        }

        for (String message : messages) {
            sender.sendMessage(message);
        }
    }
}
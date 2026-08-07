package com.nekoessentialsx.commands;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import com.nekoessentialsx.titles.TitleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commandtitle implements CommandExecutor, TabCompleter {
    private final NekoEssentialX plugin;
    private final TitleManager titleManager;

    public Commandtitle(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        // 注册Tab补全器到title和etitle命令
        plugin.getCommand("title").setTabCompleter(this);
        plugin.getCommand("etitle").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!titleManager.isEnabled()) {
                String message = "§c呜...头衔系统已经被主人关掉啦，暂时用不了的说~喵~";
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
                showHelp(sender, label);
                return true;
            }

            final String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "set":
                    setTitle(sender, args);
                    break;
                case "list":
                    listTitles(sender);
                    break;
                case "info":
                    titleInfo(sender, args);
                    break;
                case "give":
                    giveTitle(sender, args);
                    break;
                case "take":
                    takeTitle(sender, args);
                    break;
                case "clear":
                    clearTitle(sender, args);
                    break;
                case "admin":
                    handleAdminCommand(sender, args);
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
            plugin.getLogger().severe("执行title命令时发生错误: " + e.getMessage());
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
                String[] subCommands = {"set", "list", "info", "give", "take", "clear", "admin", "help"};
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(args[0].toLowerCase())) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length >= 2) {
                String subCommand = args[0].toLowerCase();
                
                if (subCommand.equals("admin")) {
                    if (args.length == 2) {
                        // 补全管理员子命令
                        String[] adminSubCommands = {"create", "edit", "delete", "toggle"};
                        for (String adminSubCommand : adminSubCommands) {
                            if (adminSubCommand.startsWith(args[1].toLowerCase())) {
                                completions.add(adminSubCommand);
                            }
                        }
                    } else if (args.length >= 3) {
                        String adminSubCommand = args[1].toLowerCase();
                        if (adminSubCommand.equals("edit") || adminSubCommand.equals("delete") || adminSubCommand.equals("toggle")) {
                            // 补全称号ID
                            for (String titleId : titleManager.getTitles().keySet()) {
                                if (titleId.startsWith(args[2].toLowerCase())) {
                                    completions.add(titleId);
                                }
                            }
                        } else if (adminSubCommand.equals("create") && args.length == 9) {
                            // 补全是否启用选项
                            String[] enabledOptions = {"true", "false"};
                            for (String option : enabledOptions) {
                                if (option.startsWith(args[8].toLowerCase())) {
                                    completions.add(option);
                                }
                            }
                        } else if (adminSubCommand.equals("edit") && args.length == 9) {
                            // 补全是否启用选项
                            String[] enabledOptions = {"true", "false"};
                            for (String option : enabledOptions) {
                                if (option.startsWith(args[8].toLowerCase())) {
                                    completions.add(option);
                                }
                            }
                        }
                    }
                } else {
                    switch (subCommand) {
                        case "set":
                        case "info":
                            // 补全头衔ID
                            if (args.length == 2) {
                                for (String titleId : titleManager.getTitles().keySet()) {
                                    TitleManager.Title title = titleManager.getTitle(titleId);
                                    if (title.isEnabled() && 
                                        (sender.hasPermission(title.getPermission()) || sender.hasPermission("nekoessentialsx.title.admin")) &&
                                        titleId.startsWith(args[1].toLowerCase())) {
                                        completions.add(titleId);
                                    }
                                }
                            }
                            break;
                        case "give":
                        case "take":
                            // 补全玩家名/选择器或头衔ID
                            if (args.length == 2) {
                                completions = com.nekoessentialsx.util.PlayerSelector.complete(sender, args[1]);
                            } else if (args.length == 3) {
                                // 补全头衔ID
                                for (String titleId : titleManager.getTitles().keySet()) {
                                    if (titleId.startsWith(args[2].toLowerCase())) {
                                        completions.add(titleId);
                                    }
                                }
                            }
                            break;
                        case "clear":
                            // 补全玩家名/选择器
                            if (args.length == 2) {
                                completions = com.nekoessentialsx.util.PlayerSelector.complete(sender, args[1]);
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Tab补全时发生错误: " + e.getMessage());
        }
        
        Collections.sort(completions);
        return completions;
    }

    private void showHelp(CommandSender sender, String label) {
        String[] messages = {
            "§6===== §e头衔系统帮助 §6=====",
            "§e/" + label + " set <头衔ID> §6- 设置自己的头衔",
            "§e/" + label + " list §6- 列出可用的头衔",
            "§e/" + label + " info <头衔ID> §6- 查看头衔信息",
            "§e/" + label + " give <玩家|@选择器> <头衔ID> §6- 授予玩家头衔",
            "§e/" + label + " take <玩家|@选择器> <头衔ID> §6- 移除玩家头衔",
            "§e/" + label + " clear [玩家|@选择器] §6- 清除玩家头衔"
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

    private void setTitle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c呜...只有玩家才可以用这个命令的说~主人没有头的说喵~");
            return;
        }

        if (args.length < 2) {
            String message = "§c呜...正确的用法是这样哦: /title set <头衔ID> 喵~";
            Player player = (Player) sender;
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        Player player = (Player) sender;
        String titleId = args[1];
        CatChatProcessor processor = CatChatProcessor.getInstance();

        if (!titleManager.getTitles().containsKey(titleId)) {
            String message = "§c呜...找不到这个头衔的说~喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        TitleManager.Title title = titleManager.getTitle(titleId);
        if (!player.hasPermission(title.getPermission()) && !player.hasPermission("nekoessentialsx.title.admin")) {
            String message = "§c呜...你没有权限戴这个头衔的说~杂鱼先乖乖的喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(player, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        titleManager.updatePlayerTitle(player, titleId);
        String message = "§a呜呼~头衔戴好成功！现在的头衔是: §6" + title.getName() + "§a 的说~超好看喵~";
        if (processor != null) {
            processor.sendCatStyleMessage(player, message);
        } else {
            sender.sendMessage(message);
        }
    }

    private void listTitles(CommandSender sender) {
        List<String> availableTitles = new ArrayList<>();

        for (TitleManager.Title title : titleManager.getTitles().values()) {
            if (title.isEnabled() && (sender.hasPermission(title.getPermission()) || sender.hasPermission("nekoessentialsx.title.admin"))) {
                availableTitles.add(title.getName());
            }
        }

        CatChatProcessor processor = CatChatProcessor.getInstance();
        if (availableTitles.isEmpty()) {
            String message = "§c呜...主人现在一个可用的头衔都没有的说~去弄一个来戴戴的喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        Collections.sort(availableTitles);
        String header = "§6可用头衔列表:喵~";
        if (sender instanceof Player && processor != null) {
            processor.sendCatStyleMessage((Player) sender, header);
            for (String titleName : availableTitles) {
                processor.sendCatStyleMessage((Player) sender, "§e- " + titleName + "§e喵~");
            }
        } else {
            sender.sendMessage(header);
            for (String titleName : availableTitles) {
                sender.sendMessage("§e- " + titleName + "§e喵~");
            }
        }
    }

    private void titleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String message = "§c呜...正确的用法是这样哦: /title info <头衔ID> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        String titleId = args[1];
        CatChatProcessor processor = CatChatProcessor.getInstance();
        if (!titleManager.getTitles().containsKey(titleId)) {
            String message = "§c呜...找不到这个头衔的说~喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        TitleManager.Title title = titleManager.getTitle(titleId);
        String[] messages = {
            "§6===== §e头衔信息 §6=====",
            "§e名称: §f" + title.getName(),
            "§eID: §f" + title.getId(),
            "§e前缀: §f" + title.getPrefix(),
            "§e后缀: §f" + title.getSuffix(),
            "§e权限: §f" + title.getPermission(),
            "§e优先级: §f" + title.getPriority(),
            "§e状态: §f" + (title.isEnabled() ? "启用" : "禁用")
        };
        
        if (sender instanceof Player && processor != null) {
            for (String message : messages) {
                processor.sendCatStyleMessage((Player) sender, message);
            }
        } else {
            for (String message : messages) {
                sender.sendMessage(message);
            }
        }
    }

    private void giveTitle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.title.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        if (args.length < 3) {
            String message = "§c呜...正确的用法是这样哦: /title give <玩家|@选择器> <头衔ID> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);
        CatChatProcessor processor = CatChatProcessor.getInstance();
        if (targets.isEmpty()) {
            String message = "§c呜...找不到这个玩家的说~喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        String titleId = args[2];
        if (!titleManager.getTitles().containsKey(titleId)) {
            String message = "§c呜...找不到这个头衔的说~喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        TitleManager.Title title = titleManager.getTitle(titleId);
        for (Player target : targets) {
            titleManager.updatePlayerTitle(target, titleId);

            String senderMessage = "§a呜呼~成功给玩家 §e" + target.getName() + " §a 戴上了头衔: §6" + title.getName() + " §a 的说~喵~";
            String targetMessage = "§a恭喜获得新头衔: §6" + title.getName() + " §a！要好好珍惜的说~喵~";

            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, senderMessage);
            } else {
                sender.sendMessage(senderMessage);
            }

            if (processor != null) {
                processor.sendCatStyleMessage(target, targetMessage);
            } else {
                target.sendMessage(targetMessage);
            }
        }
    }

    private void takeTitle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.title.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        if (args.length < 3) {
            String message = "§c呜...正确的用法是这样哦: /title take <玩家|@选择器> <头衔ID> 喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);
        CatChatProcessor processor = CatChatProcessor.getInstance();
        if (targets.isEmpty()) {
            String message = "§c找不到这个玩家！";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        String titleId = args[2];
        if (!titleManager.getTitles().containsKey(titleId)) {
            String message = "§c呜...找不到这个头衔的说~喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        TitleManager.Title title = titleManager.getTitle(titleId);
        for (Player target : targets) {
            // 使用TitleManager的clearPlayerTitle方法，确保完全清除头衔
            titleManager.clearPlayerTitle(target.getName());

            String senderMessage = "§a呜呼~成功把 §e" + target.getName() + " §a 的头衔摘下来啦: §6" + title.getName() + " §a 的说~喵~";
            String targetMessage = "§c呜...你的头衔 §6" + title.getName() + " §c 被摘掉了的说~不要太伤心喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, senderMessage);
            } else {
                sender.sendMessage(senderMessage);
            }

            if (processor != null) {
                processor.sendCatStyleMessage(target, targetMessage);
            } else {
                target.sendMessage(targetMessage);
            }
        }
    }

    private void clearTitle(CommandSender sender, String[] args) {
        CatChatProcessor processor = CatChatProcessor.getInstance();

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c呜...正确的用法是这样哦: /title clear <玩家> 喵~");
                return;
            }
            Player target = (Player) sender;
            titleManager.clearPlayerTitle(target.getName());
            String message = "§a呜呼~你的头衔被我摘下来啦，现在干干净净的喵~";
            if (processor != null) {
                processor.sendCatStyleMessage(target, message);
            } else {
                target.sendMessage(message);
            }
            return;
        }

        if (!sender.hasPermission("nekoessentialsx.title.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        java.util.List<Player> targets = com.nekoessentialsx.util.PlayerSelector.resolve(sender, args[1]);
        if (targets.isEmpty()) {
            String message = "§c找不到这个玩家！";
            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
            return;
        }

        for (Player target : targets) {
            // 使用TitleManager的clearPlayerTitle方法，确保完全清除头衔
            titleManager.clearPlayerTitle(target.getName());

            String senderMessage = "§a呜呼~成功把 §e" + target.getName() + " §a 的头衔清除干净啦的说~喵~";
            String targetMessage = "§c呜...你的头衔被我摘掉啦，现在是白板的说~喵~";

            if (sender instanceof Player && processor != null) {
                processor.sendCatStyleMessage((Player) sender, senderMessage);
            } else {
                sender.sendMessage(senderMessage);
            }

            if (processor != null) {
                processor.sendCatStyleMessage(target, targetMessage);
            } else {
                target.sendMessage(targetMessage);
            }
        }
    }
    
    /**
     * 处理管理员命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nekoessentialsx.title.admin")) {
            String message = "§c呜...主人没有权限用这条命令的说~喵~";
            if (sender instanceof Player) {
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    processor.sendCatStyleMessage((Player) sender, message);
                } else {
                    sender.sendMessage(message);
                }
            } else {
                sender.sendMessage(message);
            }
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c呜...正确的用法是这样哦: /title admin <create/edit/delete/toggle> [参数] 喵~");
            return;
        }
        
        String adminSubCommand = args[1].toLowerCase();
        switch (adminSubCommand) {
            case "create":
                createTitle(sender, args);
                break;
            case "edit":
                editTitle(sender, args);
                break;
            case "delete":
                deleteTitle(sender, args);
                break;
            case "toggle":
                toggleTitle(sender, args);
                break;
            default:
                sender.sendMessage("§c未知的管理员子命令: §e" + args[1] + "§c喵~");
                sender.sendMessage("§c呜...可用的有这些的说: create, edit, delete, toggle 喵~");
                break;
        }
    }
    
    /**
     * 创建新称号
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void createTitle(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§c用法: /title admin create <称号ID> <名称> <前缀> <后缀> <权限> <优先级> <是否启用(true/false)>喵~");
            return;
        }
        
        String titleId = args[2];
        String name = args[3];
        String prefix = args[4];
        String suffix = args[5];
        String permission = args[6];
        int priority;
        boolean enabled;
        
        try {
            priority = Integer.parseInt(args[7]);
            enabled = Boolean.parseBoolean(args[8]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c呜...优先级要是整数，启用要写true/false才行哦~喵~");
            return;
        }
        
        // 检查称号ID是否已存在
        if (titleManager.getTitle(titleId) != null) {
            sender.sendMessage("§c呜...这个称号ID已经有人用啦~换个名字的说喵~");
            return;
        }
        
        // 创建新称号
        titleManager.createTitle(titleId, name, prefix, suffix, permission, priority, enabled);
        sender.sendMessage("§a呜呼~称号创建成功啦喵~");
    }
    
    /**
     * 编辑称号
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void editTitle(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§c用法: /title admin edit <称号ID> <名称> <前缀> <后缀> <权限> <优先级> <是否启用(true/false)>喵~");
            return;
        }
        
        String titleId = args[2];
        String name = args[3];
        String prefix = args[4];
        String suffix = args[5];
        String permission = args[6];
        int priority;
        boolean enabled;
        
        try {
            priority = Integer.parseInt(args[7]);
            enabled = Boolean.parseBoolean(args[8]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c呜...优先级要是整数，启用要写true/false才行哦~喵~");
            return;
        }
        
        // 检查称号ID是否存在
        if (titleManager.getTitle(titleId) == null) {
            sender.sendMessage("§c呜...找不到这个称号ID的说~喵~");
            return;
        }
        
        // 编辑称号
        titleManager.editTitle(titleId, name, prefix, suffix, permission, priority, enabled);
        sender.sendMessage("§a呜呼~称号编辑成功啦喵~");
    }
    
    /**
     * 删除称号
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void deleteTitle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /title admin delete <称号ID>喵~");
            return;
        }
        
        String titleId = args[2];
        
        // 检查称号ID是否存在
        if (titleManager.getTitle(titleId) == null) {
            sender.sendMessage("§c呜...找不到这个称号ID的说~喵~");
            return;
        }
        
        // 删除称号
        titleManager.deleteTitle(titleId);
        sender.sendMessage("§a呜呼~称号删除成功啦喵~");
    }
    
    /**
     * 切换称号启用状态
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void toggleTitle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /title admin toggle <称号ID>喵~");
            return;
        }
        
        String titleId = args[2];
        
        // 检查称号ID是否存在
        TitleManager.Title title = titleManager.getTitle(titleId);
        if (title == null) {
            sender.sendMessage("§c呜...找不到这个称号ID的说~喵~");
            return;
        }
        
        // 切换启用状态
        boolean newEnabled = !title.isEnabled();
        titleManager.toggleTitleEnabled(titleId, newEnabled);
        sender.sendMessage("§a呜呼~称号状态切换成这样啦: §e" + (newEnabled ? "启用" : "禁用") + " §a 的说~喵~");
    }
}
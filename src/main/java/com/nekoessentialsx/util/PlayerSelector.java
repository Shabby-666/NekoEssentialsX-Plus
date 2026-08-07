package com.nekoessentialsx.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 玩家目标选择器工具。
 *
 * <p>提供与 Minecraft 内置命令一致的 <code>@</code> 选择器，
 * 以及 <code>*</code> 通配选择器，用于解析命令中的玩家参数：</p>
 * <ul>
 *   <li><code>**</code> - 所有记录在案的玩家（本插件仅在线目标，等价于 <code>*</code>）</li>
 *   <li><code>*</code>  - 所有在线玩家</li>
 *   <li><code>@a</code> - 所有在线玩家</li>
 *   <li><code>@e</code> - 所有在线玩家（本插件仅针对玩家目标）</li>
 *   <li><code>@p</code> - 距离执行者最近的玩家</li>
 *   <li><code>@s</code> - 执行者自身（仅限玩家）</li>
 *   <li><code>@r</code> - 随机一名在线玩家</li>
 *   <li>其它        - 按玩家名精确匹配（不区分大小写）</li>
 * </ul>
 */
public final class PlayerSelector {

    private PlayerSelector() {
    }

    /**
     * 解析选择器/玩家名，返回匹配到的所有在线玩家。
     *
     * @param sender 命令执行者（可为控制台）
     * @param input  选择器或玩家名
     * @return 匹配的玩家列表，可能为空
     */
    public static List<Player> resolve(CommandSender sender, String input) {
        if (input == null) {
            return Collections.emptyList();
        }

        String target = input.trim();

        // 空选择器
        if (target.isEmpty()) {
            return Collections.emptyList();
        }

        // '**' 与 '*' 均表示所有在线玩家（与 Essentials 一致：'*' 在线、'**' 全部）
        if (target.equals("**") || target.equals("*") || target.equals("@a") || target.equals("@e")) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }

        // '@s' 表示执行者自身
        if (target.equals("@s")) {
            if (sender instanceof Player) {
                return Collections.singletonList((Player) sender);
            }
            return Collections.emptyList();
        }

        // '@p' 表示距离执行者最近的玩家
        if (target.equals("@p")) {
            return Collections.singletonList(nearestPlayer(sender));
        }

        // '@r' 表示随机一名在线玩家
        if (target.equals("@r")) {
            return Collections.singletonList(randomPlayer(sender));
        }

        // 其它情况按玩家名匹配（不区分大小写），找不到时尝试前缀模糊匹配
        Player player = Bukkit.getPlayerExact(target);
        if (player != null) {
            return Collections.singletonList(player);
        }

        List<Player> matches = Bukkit.matchPlayer(target);
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        return matches;
    }

    /**
     * 解析选择器/玩家名，返回单个目标玩家；若匹配多个或匹配不到则返回首个/空。
     *
     * @param sender 命令执行者（可为控制台）
     * @param input  选择器或玩家名
     * @return 匹配的第一个玩家，找不到返回 null
     */
    public static Player resolveSingle(CommandSender sender, String input) {
        List<Player> players = resolve(sender, input);
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * 判断输入是否为多目标选择器（可能匹配到多个玩家，如 * 或 @a）。
     *
     * @param input 选择器或玩家名
     * @return 是否为多目标选择器
     */
    public static boolean isMultiTarget(String input) {
        String target = input == null ? "" : input.trim();
        return target.equals("**") || target.equals("*") || target.equals("@a") || target.equals("@e");
    }

    /**
     * 获取某个未知目标对应的自然语言表述，用于提示信息。
     *
     * @param input 原始输入
     * @return 描述文本
     */
    public static String describe(String input) {
        String target = input.trim();
        if (target.equals("**") || target.equals("*")) {
            return "所有玩家";
        }
        if (target.equals("@a") || target.equals("@e")) {
            return "所有在线玩家";
        }
        if (target.equals("@p")) {
            return "最近的玩家";
        }
        if (target.equals("@s")) {
            return "你";
        }
        if (target.equals("@r")) {
            return "一名随机玩家";
        }
        return target;
    }

    /**
     * 补全选择器和在线玩家名。
     *
     * @param sender  命令执行者
     * @param partial 已输入的部分
     * @return 匹配的补全建议
     */
    public static List<String> complete(CommandSender sender, String partial) {
        String prefix = partial == null ? "" : partial.toLowerCase();
        List<String> completions = new ArrayList<>();

        // 选择器建议
        String[] selectors = {"@a", "@p", "@s", "@r"};
        for (String selector : selectors) {
            if (selector.toLowerCase().startsWith(prefix)) {
                completions.add(selector);
            }
        }
        // 通配符建议（与 Essentials 一致，提供 * 与 **）
        if ("*".startsWith(prefix)) {
            completions.add("*");
        }
        if ("**".startsWith(prefix)) {
            completions.add("**");
        }

        // 在线玩家名建议
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase().startsWith(prefix)) {
                completions.add(name);
            }
        }

        return completions;
    }

    private static Player nearestPlayer(CommandSender sender) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (sender instanceof Player) {
            Player self = (Player) sender;
            // 仅剩自己时返回自己，否则从其它玩家中找最近
            if (online.size() <= 1) {
                return self;
            }
            online.removeIf(player -> player.getUniqueId().equals(self.getUniqueId()));
            Location loc = self.getLocation();
            Player nearest = null;
            double best = Double.MAX_VALUE;
            for (Player p : online) {
                double dist = p.getLocation().distanceSquared(loc);
                if (dist < best) {
                    best = dist;
                    nearest = p;
                }
            }
            return nearest;
        }

        return online.isEmpty() ? null : online.get(0);
    }

    private static Player randomPlayer(CommandSender sender) {
        List<Player> all = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (all.isEmpty()) {
            return null;
        }
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }
}
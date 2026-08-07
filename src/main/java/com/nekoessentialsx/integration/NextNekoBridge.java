package com.nekoessentialsx.integration;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * NextNeko 桥接器
 * 通过反射调用 NextNeko 的公开 API，避免两个插件产生编译期耦合。
 * 只有在服务器安装并启用了 NextNeko 时才会真正生效。
 */
public class NextNekoBridge {

    public static final String NEXT_NEKO_PLUGIN_NAME = "NextNeko";

    private final NekoEssentialX plugin;
    private JavaPlugin nextNekoPlugin;
    private Object nekoManager;
    private Object playerConfigManager;
    private Object climbCommand;

    public NextNekoBridge(NekoEssentialX plugin) {
        this.plugin = plugin;
        scan();
    }

    /**
     * 重新扫描（启动/重载时调用）
     */
    public void scan() {
        this.nextNekoPlugin = null;
        this.nekoManager = null;
        this.playerConfigManager = null;
        this.climbCommand = null;

        Plugin p = Bukkit.getPluginManager().getPlugin(NEXT_NEKO_PLUGIN_NAME);
        if (p != null && p.isEnabled()) {
            try {
                this.nextNekoPlugin = (JavaPlugin) p;
                this.nekoManager = invoke(p, "getNekoManager", new Class<?>[0], new Object[0]);
                this.playerConfigManager = invoke(p, "getPlayerConfigManager", new Class<?>[0], new Object[0]);
                this.climbCommand = invoke(p, "getClimbCommand", new Class<?>[0], new Object[0]);
            } catch (Exception e) {
                plugin.getLogger().warning("扫描 NextNeko 桥接组件失败: " + e.getMessage());
                this.nextNekoPlugin = null;
            }
        }
    }

    /**
     * NextNeko 是否已安装并启用
     */
    public boolean isInstalled() {
        return nextNekoPlugin != null;
    }

    /**
     * 获取 NextNeko 插件实例（仅当已安装时有效）
     */
    public JavaPlugin getPlugin() {
        return nextNekoPlugin;
    }

    // ==================== 配置读写 ====================

    /**
     * 获取 NextNeko 的配置文件（用于编辑只能改配置文件的内容）
     */
    public FileConfiguration getConfig() {
        if (!isInstalled()) {
            return null;
        }
        return nextNekoPlugin.getConfig();
    }

    /**
     * 保存 NextNeko 配置
     */
    public void saveConfig() {
        if (!isInstalled()) {
            return;
        }
        nextNekoPlugin.saveConfig();
    }

    /**
     * 重载 NextNeko 配置
     */
    public void reloadConfig() {
        if (!isInstalled()) {
            return;
        }
        nextNekoPlugin.reloadConfig();
    }

    // ==================== 猫娘相关 ====================

    public boolean isNeko(Player player) {
        Boolean r = (Boolean) invokeManager(nekoManager, "isNeko", new Class<?>[]{Player.class}, new Object[]{player});
        return r != null && r;
    }

    public boolean isNeko(String playerName) {
        Boolean r = (Boolean) invokeManager(nekoManager, "isNeko", new Class<?>[]{String.class}, new Object[]{playerName});
        return r != null && r;
    }

    public Set<String> getAllNekoNames() {
        Object r = invokeManager(nekoManager, "getAllNekoNames", new Class<?>[0], new Object[0]);
        if (r instanceof Set) {
            Set<String> names = new HashSet<>();
            for (Object o : (Set<?>) r) {
                names.add(String.valueOf(o));
            }
            return names;
        }
        return new HashSet<>();
    }

    /**
     * 直接设置玩家为猫娘（绕过请求流程，供管理员使用）
     */
    public void setNekoDirect(String playerName, boolean isNeko) {
        invokeManager(nekoManager, "setNekoDirect", new Class<?>[]{String.class, boolean.class}, new Object[]{playerName, isNeko});
    }

    // ==================== 主人与猫娘关系 ====================

    public void addOwner(Player neko, Player owner) {
        invokeManager(nekoManager, "addOwner", new Class<?>[]{Player.class, Player.class}, new Object[]{neko, owner});
    }

    public void addOwnerByName(String nekoName, String ownerName) {
        invokeManager(nekoManager, "addOwnerByName", new Class<?>[]{String.class, String.class}, new Object[]{nekoName, ownerName});
    }

    public void addOwnerDirect(String nekoName, String ownerName) {
        invokeManager(nekoManager, "addOwnerDirect", new Class<?>[]{String.class, String.class}, new Object[]{nekoName, ownerName});
    }

    public void removeOwner(Player neko, Player owner) {
        invokeManager(nekoManager, "removeOwner", new Class<?>[]{Player.class, Player.class}, new Object[]{neko, owner});
    }

    public void removeOwnerDirect(String nekoName, String ownerName) {
        invokeManager(nekoManager, "removeOwnerDirect", new Class<?>[]{String.class, String.class}, new Object[]{nekoName, ownerName});
    }

    public boolean isOwnerOf(String ownerName, String nekoName) {
        Boolean r = (Boolean) invokeManager(nekoManager, "isOwnerOf", new Class<?>[]{String.class, String.class}, new Object[]{ownerName, nekoName});
        return r != null && r;
    }

    public boolean hasOwner(String playerName) {
        Boolean r = (Boolean) invokeManager(nekoManager, "hasOwner", new Class<?>[]{String.class}, new Object[]{playerName});
        return r != null && r;
    }

    public Set<String> getOwnerNames(String nekoName) {
        Object r = invokeManager(nekoManager, "getOwnerNames", new Class<?>[]{String.class}, new Object[]{nekoName});
        if (r instanceof Set) {
            Set<String> names = new HashSet<>();
            for (Object o : (Set<?>) r) {
                names.add(String.valueOf(o));
            }
            return names;
        }
        return new HashSet<>();
    }

    public Set<String> getNekoNamesByOwner(String ownerName) {
        Object r = invokeManager(playerConfigManager, "getNekoNamesByOwner", new Class<?>[]{String.class}, new Object[]{ownerName});
        if (r instanceof Set) {
            Set<String> names = new HashSet<>();
            for (Object o : (Set<?>) r) {
                names.add(String.valueOf(o));
            }
            return names;
        }
        return new HashSet<>();
    }

    public boolean wouldCreateCycle(String nekoName, String ownerName) {
        Boolean r = (Boolean) invokeManager(nekoManager, "wouldCreateCycle", new Class<?>[]{String.class, String.class}, new Object[]{nekoName, ownerName});
        return r != null && r;
    }

    // ==================== 主人申请 ====================

    public boolean hasOwnerRequest(Player requester, Player neko) {
        Boolean r = (Boolean) invokeManager(nekoManager, "hasOwnerRequest", new Class<?>[]{Player.class, Player.class}, new Object[]{requester, neko});
        return r != null && r;
    }

    public void sendOwnerRequest(Player requester, Player neko) {
        invokeManager(nekoManager, "sendOwnerRequest", new Class<?>[]{Player.class, Player.class}, new Object[]{requester, neko});
    }

    public void acceptOwnerRequest(Player requester, Player neko) {
        invokeManager(nekoManager, "acceptOwnerRequest", new Class<?>[]{Player.class, Player.class}, new Object[]{requester, neko});
    }

    public void denyOwnerRequest(Player requester, Player neko) {
        invokeManager(nekoManager, "denyOwnerRequest", new Class<?>[]{Player.class, Player.class}, new Object[]{requester, neko});
    }

    @SuppressWarnings("unchecked")
    public Set<Player> getOwnerRequests(Player neko) {
        Object r = invokeManager(nekoManager, "getOwnerRequests", new Class<?>[]{Player.class}, new Object[]{neko});
        if (r instanceof Set) {
            Set<Player> players = new HashSet<>();
            for (Object o : (Set<?>) r) {
                if (o instanceof Player) {
                    players.add((Player) o);
                }
            }
            return players;
        }
        return new HashSet<>();
    }

    // ==================== 技能开关（只能用命令切换的配置） ====================

    public boolean isClimbEnabled(Player player) {
        if (climbCommand == null) {
            return true;
        }
        Boolean r = (Boolean) invokeManager(climbCommand, "getClimbStatus", new Class<?>[]{Player.class}, new Object[]{player});
        return r == null || r;
    }

    public void setClimbEnabled(Player player, boolean enabled) {
        if (climbCommand == null) {
            return;
        }
        invokeManager(climbCommand, "setClimbStatus", new Class<?>[]{Player.class, boolean.class}, new Object[]{player, enabled});
    }

    public void toggleClimb(Player player) {
        if (climbCommand == null) {
            return;
        }
        invokeManager(climbCommand, "toggleClimb", new Class<?>[]{Player.class}, new Object[]{player});
    }

    public boolean isTailPullEnabled(Player player) {
        Boolean r = (Boolean) invokeManager(nekoManager, "isTailPullEnabled", new Class<?>[]{Player.class}, new Object[]{player});
        return r == null || r;
    }

    public void setTailPullEnabled(Player player, boolean enabled) {
        invokeManager(nekoManager, "setTailPullEnabled", new Class<?>[]{Player.class, boolean.class}, new Object[]{player, enabled});
    }

    public boolean isNoticeEnabled(Player player) {
        Boolean r = (Boolean) invokeManager(playerConfigManager, "isNoticeEnabled", new Class<?>[]{Player.class}, new Object[]{player});
        return r == null || r;
    }

    public void setNoticeEnabledDirect(String playerName, boolean enabled) {
        invokeManager(playerConfigManager, "setNoticeEnabledDirect", new Class<?>[]{String.class, boolean.class}, new Object[]{playerName, enabled});
    }

    // ==================== 反射工具 ====================

    private Object invokeManager(Object target, String method, Class<?>[] types, Object[] args) {
        if (target == null) {
            return null;
        }
        return invoke(target, method, types, args);
    }

    private Object invoke(Object target, String method, Class<?>[] types, Object[] args) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(method, types);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("调用 NextNeko 方法 " + method + " 失败: " + e.getMessage());
            return null;
        }
    }
}
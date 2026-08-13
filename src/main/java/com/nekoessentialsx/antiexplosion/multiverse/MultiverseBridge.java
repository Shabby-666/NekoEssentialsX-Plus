package com.nekoessentialsx.antiexplosion.multiverse;

import org.bukkit.Bukkit;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multiverse-Core (MV5) 桥接层。
 *
 * <p>软依赖：服务器未安装 Multiverse-Core 时，所有方法安全返回空值，
 * 不会抛出 NoClassDefFoundError。安装后可将 Multiverse 自定义世界
 * （含未加载世界）纳入防爆系统的维度列表。</p>
 */
public final class MultiverseBridge {

    private static volatile boolean available = false;
    private static volatile String defaultWorldName = null;

    private MultiverseBridge() {
    }

    /**
     * 检测 Multiverse-Core 是否已加载并初始化 API。
     * 应在插件 onEnable 时调用。
     */
    public static void init() {
        available = false;
        defaultWorldName = null;
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") == null) {
            return;
        }
        try {
            MultiverseCoreApi api = MultiverseCoreApi.get();
            // 服务器默认世界（server.properties 的 level-name）
            defaultWorldName = api.getWorldManager().getDefaultWorld()
                    .map(MultiverseWorld::getName)
                    .getOrNull();
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * 判断是否为服务器内置维度（默认世界及其下界/末地）。
     */
    public static boolean isBuiltInWorld(String worldName) {
        if (worldName == null || defaultWorldName == null || defaultWorldName.isEmpty()) {
            return false;
        }
        return worldName.equals(defaultWorldName)
                || worldName.equals(defaultWorldName + "_nether")
                || worldName.equals(defaultWorldName + "_the_end");
    }

    /**
     * 判断某世界是否为 Multiverse 自定义维度（可能未加载）。
     * 服务器内置维度（默认世界/下界/末地）不算。
     */
    public static boolean isMultiverseWorld(String worldName) {
        if (!available || isBuiltInWorld(worldName)) {
            return false;
        }
        try {
            return MultiverseCoreApi.get().getWorldManager().isWorld(worldName);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 获取所有 Multiverse 自定义维度的真实世界名（含未加载，排除内置维度）。
     */
    public static List<String> getWorldNames() {
        if (!available) {
            return Collections.emptyList();
        }
        try {
            List<String> names = new ArrayList<>();
            for (MultiverseWorld world : MultiverseCoreApi.get().getWorldManager().getWorlds()) {
                String name = world.getName();
                if (!isBuiltInWorld(name)) {
                    names.add(name);
                }
            }
            return names;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取世界别名（无别名则回退世界名）；不是 MV 自定义维度返回 null。
     */
    public static String getAliasOrName(String worldName) {
        if (!available || isBuiltInWorld(worldName)) {
            return null;
        }
        try {
            return MultiverseCoreApi.get().getWorldManager().getWorld(worldName)
                    .map(MultiverseWorld::getAliasOrName)
                    .getOrElse(worldName);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 判断 MV 自定义维度当前是否已加载；不是 MV 世界返回 false。
     */
    public static boolean isWorldLoaded(String worldName) {
        if (!available || isBuiltInWorld(worldName)) {
            return false;
        }
        try {
            return MultiverseCoreApi.get().getWorldManager().getWorld(worldName)
                    .map(MultiverseWorld::isLoaded)
                    .getOrElse(false);
        } catch (Throwable t) {
            return false;
        }
    }
}

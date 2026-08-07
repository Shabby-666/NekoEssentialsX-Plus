package com.nekoessentialsx.back;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /back 位置记录管理器
 * 记录每个玩家最近一次传送/死亡前的位置，仅保留最近一次。
 */
public class BackManager {
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    /**
     * 记录玩家最近一次的位置（覆盖旧记录，只保留最近一次）
     */
    public void set(Player player, Location location) {
        if (location == null) {
            return;
        }
        lastLocations.put(player.getUniqueId(), location.clone());
    }

    /**
     * 获取玩家最近的记录位置
     * @return 若没有记录则返回 null
     */
    public Location get(Player player) {
        return lastLocations.get(player.getUniqueId());
    }

    /**
     * 移除玩家的记录（/back 使用后清除，避免重复返回）
     */
    public void clear(Player player) {
        lastLocations.remove(player.getUniqueId());
    }

    /**
     * 玩家是否有有效的回传记录
     */
    public boolean has(Player player) {
        Location loc = lastLocations.get(player.getUniqueId());
        return loc != null && loc.getWorld() != null;
    }

    /**
     * 移除下线玩家的记录，避免内存占用
     */
    public void cleanup(Player player) {
        lastLocations.remove(player.getUniqueId());
    }
}
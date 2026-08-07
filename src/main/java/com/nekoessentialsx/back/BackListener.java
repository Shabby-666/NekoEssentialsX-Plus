package com.nekoessentialsx.back;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * /back 位置记录监听器
 * 在玩家通过传送（tpa/tp/home/warp等）或死亡时记录其移动前的位置。
 * 监听PlayerTeleportEvent可以在各种传送后自动记录，无需逐个修改传送点。
 */
public class BackListener implements Listener {
    private final BackManager backManager;

    // 正在执行 /back 自身的传送，避免递归记录
    private final Set<UUID> backTeleporting = new HashSet<>();

    public BackListener(BackManager backManager) {
        this.backManager = backManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // 若该传送是 /back 自己发起的，不重复记录
        if (backTeleporting.contains(player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        // 仅当传送到不同位置时才记录（避免同坐标传送覆盖掉有效回传点）
        if (sameBlock(from, to)) {
            return;
        }
        // 记录传送前的位置
        backManager.set(player, from);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // 记录死亡位置，便于 /back 回到死前
        backManager.set(player, player.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 玩家下线时清理，避免内存占用
        backManager.cleanup(event.getPlayer());
    }

    /**
     * 标记某玩家正在进行/back回传（防止记录到回传动作本身）
     */
    public void beginBack(Player player) {
        backTeleporting.add(player.getUniqueId());
    }

    /**
     * /back 回传完成后解除标记
     */
    public void endBack(Player player) {
        backTeleporting.remove(player.getUniqueId());
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
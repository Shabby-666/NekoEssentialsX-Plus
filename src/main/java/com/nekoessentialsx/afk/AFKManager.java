package com.nekoessentialsx.afk;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AFKManager implements Listener {
    private final NekoEssentialX plugin;
    private final Set<Player> afkPlayers = new HashSet<>();
    private final Map<Player, Long> lastActivityTimes = new HashMap<>();
    private long afkTimeout = 300; // 默认5分钟后自动AFK

    public AFKManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.afkTimeout = plugin.getConfig().getLong("afk.timeout", 300);
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // 启动自动AFK检测任务
        startAFKCheckTask();
    }

    /**
     * 启动自动AFK检测任务
     */
    private void startAFKCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!afkPlayers.contains(player)) {
                        long lastActivityTime = lastActivityTimes.getOrDefault(player, currentTime);
                        long inactiveTime = (currentTime - lastActivityTime) / 1000;
                        
                        if (inactiveTime >= afkTimeout) {
                            setAFK(player, true);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20); // 每秒检查一次
    }

    /**
     * 设置玩家AFK状态
     * @param player 玩家
     * @param isAFK 是否AFK
     */
    public void setAFK(Player player, boolean isAFK) {
        if (isAFK && !afkPlayers.contains(player)) {
            afkPlayers.add(player);
            Bukkit.broadcastMessage("§e" + player.getName() + " §a 猫咪跑开去挂机啦的说~喵~");
            player.setDisplayName("§7[AFK] §e" + player.getName());
        } else if (!isAFK && afkPlayers.contains(player)) {
            afkPlayers.remove(player);
            Bukkit.broadcastMessage("§e" + player.getName() + " §a 猫咪回来啦，不再是AFK的说~喵~");
            player.setDisplayName(player.getName());
        }
    }

    /**
     * 检查玩家是否为AFK状态
     * @param player 玩家
     * @return 是否AFK
     */
    public boolean isAFK(Player player) {
        return afkPlayers.contains(player);
    }

    /**
     * 更新玩家活动时间
     * @param player 玩家
     */
    public void updateActivity(Player player) {
        lastActivityTimes.put(player, System.currentTimeMillis());
        // 如果玩家之前是AFK状态，自动设置为非AFK
        if (isAFK(player)) {
            setAFK(player, false);
        }
    }

    /**
     * 处理玩家移动事件，更新活动时间
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        updateActivity(player);
    }

    /**
     * 处理玩家退出事件，清理AFK状态
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        afkPlayers.remove(player);
        lastActivityTimes.remove(player);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        this.afkTimeout = plugin.getConfig().getLong("afk.timeout", 300);
    }
}
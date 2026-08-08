package com.nekoessentialsx.antiexplosion.listener;

import com.nekoessentialsx.antiexplosion.AntiExplosionModule;
import com.nekoessentialsx.antiexplosion.manager.ExplosionProtectionManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * 防爆监听器。
 *
 * <p>「允许破坏方块」「允许伤害玩家」「允许伤害生物」三个开关彼此独立：</p>
 * <ul>
 *   <li>禁止破坏方块 → 只清理爆炸破坏的方块列表（{@code blockList().clear()}），
 *       不取消事件，爆炸伤害照常结算；</li>
 *   <li>禁止伤害 → 只在伤害事件里拦截对应目标（玩家/生物），不取消爆炸事件，
 *       方块照常被破坏；</li>
 *   <li>仅当三个开关全部关闭时，才直接取消整个爆炸事件。</li>
 * </ul>
 */
public class ExplosionProtectionListener implements Listener {
    private final AntiExplosionModule module;
    private final ExplosionProtectionManager manager;

    public ExplosionProtectionListener(AntiExplosionModule module, ExplosionProtectionManager manager) {
        this.module = module;
        this.manager = manager;
    }

    /**
     * 处理实体爆炸事件（苦力怕、TNT、末影水晶、龙息等）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!manager.isEnabled()) {
            return;
        }

        String worldName = getWorldName(event.getLocation());
        String sourceKey = identifyExplosionSource(event.getEntity(), event.getLocation());
        if (sourceKey == null) {
            return;
        }

        ExplosionProtectionManager.SourceConfig config = manager.getSource(worldName, sourceKey);
        if (!config.isEnabled()) {
            return;
        }

        String detailType = event.getEntity() == null ? sourceKey : event.getEntity().getType().toString().toLowerCase();

        // 不允许破坏方块：只清空方块列表，不取消事件，伤害照常结算
        if (!config.isBreakBlocks()) {
            event.blockList().clear();
        }

        // 三个开关全部关闭才整体取消爆炸
        boolean blocked = false;
        if (config.isFullyProtected()) {
            event.setCancelled(true);
            blocked = true;
        }

        manager.logExplosion(sourceKey, detailType, blocked,
                String.format("世界: %s, 位置: %s, 威力: %.2f, 方块数量: %d",
                        worldName, event.getLocation(), event.getYield(), event.blockList().size()));
    }

    /**
     * 处理方块爆炸事件（TNT 方块、床、重生锚等）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!manager.isEnabled()) {
            return;
        }

        String worldName = getWorldName(event.getBlock().getLocation());
        String sourceKey = identifyBlockSource(event.getBlock());

        ExplosionProtectionManager.SourceConfig config = manager.getSource(worldName, sourceKey);
        if (!config.isEnabled()) {
            return;
        }

        if (!config.isBreakBlocks()) {
            event.blockList().clear();
        }

        boolean blocked = false;
        if (config.isFullyProtected()) {
            event.setCancelled(true);
            blocked = true;
        }

        manager.logExplosion(sourceKey, sourceKey + "-block", blocked,
                String.format("世界: %s, 位置: %s, 方块数量: %d",
                        worldName, event.getBlock().getLocation(), event.blockList().size()));
    }

    /**
     * 处理爆炸触发事件（用于调整爆炸威力与范围）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!manager.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        String sourceKey = identifyExplosionSource(entity, entity.getLocation());
        if (sourceKey == null) {
            return;
        }

        ExplosionProtectionManager.SourceConfig config =
                manager.getSource(getWorldName(entity.getLocation()), sourceKey);
        if (!config.isEnabled()) {
            return;
        }

        // 完全受保护时才取消爆炸（否则会连带抹掉允许的伤害）
        if (config.isFullyProtected()) {
            event.setCancelled(true);
            manager.logExplosion(sourceKey, "explosion-prime", true,
                    String.format("世界: %s, 位置: %s, 半径: %.2f",
                            getWorldName(entity.getLocation()), entity.getLocation(), event.getRadius()));
            return;
        }

        // 调整爆炸威力
        if (config.getPowerMultiplier() != 1.0) {
            event.setRadius((float) (event.getRadius() * config.getPowerMultiplier()));
        }

        // 限制爆炸范围
        if (config.getMaxRadius() > 0 && event.getRadius() > config.getMaxRadius()) {
            event.setRadius((float) config.getMaxRadius());
        }

        event.setFire(false);
    }

    /**
     * 处理实体爆炸造成的伤害（苦力怕、TNT 实体等）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!manager.isEnabled() || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }

        String sourceKey = identifyExplosionSource(event.getDamager(), null);
        if (sourceKey == null) {
            return;
        }
        handleExplosionDamage(event, event.getEntity(), sourceKey);
    }

    /**
     * 处理方块爆炸造成的伤害（床、重生锚等）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
        if (!manager.isEnabled() || event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Block damager = event.getDamager();
        String sourceKey = damager == null ? "other" : identifyBlockSource(damager);
        handleExplosionDamage(event, event.getEntity(), sourceKey);
    }

    /**
     * 按来源配置拦截爆炸伤害：玩家看 damage-players，其他生物看 damage-entities
     */
    private void handleExplosionDamage(EntityDamageEvent event, Entity victim, String sourceKey) {
        String worldName = getWorldName(victim.getLocation());
        ExplosionProtectionManager.SourceConfig config = manager.getSource(worldName, sourceKey);
        if (!config.isEnabled()) {
            return;
        }

        boolean isPlayer = victim instanceof Player;
        boolean allowed = isPlayer ? config.isDamagePlayers() : config.isDamageEntities();
        if (allowed) {
            return;
        }

        event.setCancelled(true);
        manager.logExplosion(sourceKey, isPlayer ? "damage-player" : "damage-entity", true,
                String.format("世界: %s, 目标: %s, 位置: %s",
                        worldName, victim.getType().toString().toLowerCase(), victim.getLocation()));
    }

    /**
     * 处理实体直接改变方块（凋零、末影龙等破坏方块）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!manager.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        Block block = event.getBlock();
        ExplosionProtectionManager.EntityBlockBreakConfig config =
                manager.getBlockBreakConfig(getWorldName(block.getLocation()));
        String entityType = entity.getType().toString().toLowerCase();

        if (!config.isEnabled()) {
            return;
        }

        boolean isEntityAffected = config.isApplyToAllEntities()
                || config.getTypes().contains(entityType);
        if (!isEntityAffected) {
            return;
        }

        boolean shouldAllowBreak = config.isAllowBreak();

        // 方块类型限制
        String blockType = block.getType().name().toLowerCase();
        if (!shouldAllowBreak && !config.getAllowedBlocks().isEmpty()
                && config.getAllowedBlocks().contains(blockType)) {
            shouldAllowBreak = true;
        }
        if (config.getBlockedBlocks().contains(blockType)) {
            shouldAllowBreak = false;
        }

        if (!shouldAllowBreak) {
            event.setCancelled(true);
            manager.logExplosion("entity-block-break", entityType, true,
                    String.format("世界: %s, 位置: %s, 方块类型: %s, 实体类型: %s",
                            getWorldName(block.getLocation()), block.getLocation(), blockType, entityType));
        }
    }

    /**
     * 处理无玩家参与的方块破坏事件（如末影龙撞击破坏方块）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isEnabled() || event.getPlayer() != null) {
            return;
        }

        ExplosionProtectionManager.EntityBlockBreakConfig config =
                manager.getBlockBreakConfig(getWorldName(event.getBlock().getLocation()));
        if (config.isEnabled() && config.isApplyToAllEntities() && !config.isAllowBreak()) {
            event.setCancelled(true);
            manager.logExplosion("block-break-no-player", "unknown", true,
                    String.format("世界: %s, 位置: %s, 方块类型: %s",
                            getWorldName(event.getBlock().getLocation()),
                            event.getBlock().getLocation(),
                            event.getBlock().getType().name().toLowerCase()));
        }
    }

    /**
     * 识别爆炸来源（实体）
     */
    private String identifyExplosionSource(Entity entity, Location fallback) {
        if (entity instanceof Creeper) {
            return "creeper";
        }
        if (entity instanceof Wither) {
            return "wither";
        }
        if (entity instanceof WitherSkull) {
            return "wither";
        }
        if (entity instanceof EnderDragon) {
            return "ender-dragon";
        }
        if (entity instanceof DragonFireball) {
            return "ender-dragon";
        }
        if (entity instanceof LargeFireball) {
            return "ghast-fireball";
        }
        if (entity instanceof SmallFireball) {
            return "blaze-fireball";
        }
        if (entity instanceof TNTPrimed || entity.getType().name().equals("TNT_MINECART")) {
            return "tnt";
        }
        if (entity instanceof EnderCrystal) {
            return "end-crystal";
        }
        if (entity == null) {
            // 床/重生锚等以方块形式爆炸的事件，从位置推断来源
            if (fallback != null && fallback.getWorld() != null) {
                return identifyBlockSource(fallback.getBlock());
            }
            return null;
        }
        // 风弹、风弹（1.20.5+），按类型名匹配以兼容旧版本API
        String name = entity.getType().name();
        if (name.equals("WIND") || name.equals("WIND_CHARGE")) {
            return "wind";
        }
        return "other";
    }

    /**
     * 识别爆炸来源（方块）
     */
    private String identifyBlockSource(Block block) {
        String name = block.getType().name();
        if (name.equals("TNT") || name.equals("TNT_MINECART")) {
            return "tnt";
        }
        if (name.equals("RESPAWN_ANCHOR")) {
            return "respawn-anchor";
        }
        if (name.endsWith("_BED")) {
            return "bed";
        }
        return "other";
    }

    private String getWorldName(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() : "default";
    }
}

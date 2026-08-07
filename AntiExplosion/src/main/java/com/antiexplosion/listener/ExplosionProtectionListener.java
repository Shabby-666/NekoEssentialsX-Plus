package com.antiexplosion.listener;

import com.antiexplosion.AntiExplosion;
import com.antiexplosion.manager.ExplosionProtectionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;

public class ExplosionProtectionListener implements Listener {
    private final AntiExplosion plugin;
    private final ExplosionProtectionManager explosionManager;

    public ExplosionProtectionListener(AntiExplosion plugin, ExplosionProtectionManager explosionManager) {
        this.plugin = plugin;
        this.explosionManager = explosionManager;
    }

    /**
     * 处理实体爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        String explosionType = identifyExplosionType(entity);

        // 检查爆炸类型并应用相应的配置
        switch (explosionType) {
            case "entity-explosion":
                ExplosionProtectionManager.ExplosionConfig entityConfig = explosionManager.getEntityExplosionConfig();
                if (entityConfig.isEnabled()) {
                    String entityType = entity.getType().toString().toLowerCase();
                    if (entityConfig.getTypes().isEmpty() || entityConfig.getTypes().contains(entityType)) {
                        applyExplosionConfig(event, entityConfig, "entity-explosion", entityType);
                    }
                }
                break;
            case "tnt-explosion":
                handleTNTExplosion(event);
                break;
            case "end-crystal-explosion":
                handleEndCrystalExplosion(event);
                break;
            case "bed-explosion":
                handleBedExplosion(event);
                break;
            default:
                handleOtherExplosion(event);
                break;
        }
    }

    /**
     * 处理方块爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }

        Block block = event.getBlock();
        String explosionType = identifyBlockExplosionType(block);

        // 检查爆炸类型并应用相应的配置
        switch (explosionType) {
            case "tnt-explosion":
                handleTNTBlockExplosion(event);
                break;
            default:
                handleOtherBlockExplosion(event);
                break;
        }
    }

    /**
     * 处理实体改变方块事件（凋零、末影龙等破坏方块）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        Block block = event.getBlock();
        ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
        String entityType = entity.getType().toString().toLowerCase();
        String blockType = block.getType().name().toLowerCase();
        
        // 记录所有实体改变方块事件，用于调试
        explosionManager.logExplosion("entity-block-break-debug", entityType, false, 
                String.format("位置: %s, 方块类型: %s, 实体类型: %s, 新方块类型: %s",
                        block.getLocation(), blockType, entityType, event.getTo().name().toLowerCase()));

        if (blockBreakConfig.isEnabled()) {
            // 检查是否应用于该实体
            boolean isEntityAffected = false;
            if (blockBreakConfig.isApplyToAllEntities()) {
                isEntityAffected = true;
            } else if (blockBreakConfig.getTypes().contains(entityType)) {
                isEntityAffected = true;
            }
            
            if (isEntityAffected) {
                // 检查是否允许破坏
                boolean shouldAllowBreak = blockBreakConfig.isAllowBreak();
                
                // 检查方块类型限制
                if (!shouldAllowBreak) {
                    // 检查是否在允许列表中
                    if (!blockBreakConfig.getAllowedBlocks().isEmpty() && blockBreakConfig.getAllowedBlocks().contains(blockType)) {
                        shouldAllowBreak = true;
                    }
                    
                    // 检查是否在禁止列表中
                    if (blockBreakConfig.getBlockedBlocks().contains(blockType)) {
                        shouldAllowBreak = false;
                    }
                }
                
                // 检查范围限制
                if (shouldAllowBreak && blockBreakConfig.getMaxRange() > 0.0) {
                    // 这里可以根据需要实现范围检查逻辑
                }
                
                // 如果不允许破坏，取消事件
                if (!shouldAllowBreak) {
                    event.setCancelled(true);
                    explosionManager.logExplosion("entity-block-break", entityType, true, 
                            String.format("位置: %s, 方块类型: %s, 实体类型: %s",
                                    block.getLocation(), blockType, entityType));
                }
            }
        }
    }
    
    /**
     * 处理实体损伤事件（用于处理末影龙撞击破坏方块）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }
        
        // 检查是否是末影龙造成的伤害
        Entity damager = event.getDamager();
        if (damager instanceof EnderDragon) {
            ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
            
            if (blockBreakConfig.isEnabled()) {
                String entityType = "ender_dragon";
                
                // 检查是否应用于该实体
                boolean isEntityAffected = false;
                if (blockBreakConfig.isApplyToAllEntities()) {
                    isEntityAffected = true;
                } else if (blockBreakConfig.getTypes().contains(entityType)) {
                    isEntityAffected = true;
                }
                
                if (isEntityAffected && !blockBreakConfig.isAllowBreak()) {
                    // 取消末影龙造成的伤害
                    event.setCancelled(true);
                    explosionManager.logExplosion("entity-damage", entityType, true, 
                            String.format("位置: %s, 目标: %s",
                                    event.getEntity().getLocation(), event.getEntity().getType().toString().toLowerCase()));
                }
            }
        }
    }
    
    /**
     * 处理末影龙特定事件（用于处理末影龙破坏方块）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon) {
            ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
            
            if (blockBreakConfig.isEnabled()) {
                String entityType = "ender_dragon";
                
                // 检查是否应用于该实体
                boolean isEntityAffected = false;
                if (blockBreakConfig.isApplyToAllEntities()) {
                    isEntityAffected = true;
                } else if (blockBreakConfig.getTypes().contains(entityType)) {
                    isEntityAffected = true;
                }
                
                if (isEntityAffected && !blockBreakConfig.isAllowBreak()) {
                    // 取消末影龙的交互事件
                    event.setCancelled(true);
                    explosionManager.logExplosion("entity-interact", entityType, true, 
                            String.format("位置: %s",
                                    entity.getLocation()));
                }
            }
        }
    }
    
    /**
     * 处理末影龙破坏方块的特殊情况（使用ExplosionPrimeEvent处理）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderDragonExplosionPrime(ExplosionPrimeEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon) {
            ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
            
            if (blockBreakConfig.isEnabled()) {
                String entityType = "ender_dragon";
                
                // 检查是否应用于该实体
                boolean isEntityAffected = false;
                if (blockBreakConfig.isApplyToAllEntities()) {
                    isEntityAffected = true;
                } else if (blockBreakConfig.getTypes().contains(entityType)) {
                    isEntityAffected = true;
                }
                
                if (isEntityAffected && !blockBreakConfig.isAllowBreak()) {
                    // 取消末影龙的爆炸触发事件
                    event.setCancelled(true);
                    explosionManager.logExplosion("ender-dragon-explosion-prime", entityType, true, 
                            String.format("位置: %s, 半径: %.2f",
                                    entity.getLocation(), event.getRadius()));
                }
            }
        }
    }
    
    /**
     * 处理末影龙破坏方块的特殊情况（使用EntityExplodeEvent处理）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderDragonExplode(EntityExplodeEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }
        
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon) {
            ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
            
            if (blockBreakConfig.isEnabled()) {
                String entityType = "ender_dragon";
                
                // 检查是否应用于该实体
                boolean isEntityAffected = false;
                if (blockBreakConfig.isApplyToAllEntities()) {
                    isEntityAffected = true;
                } else if (blockBreakConfig.getTypes().contains(entityType)) {
                    isEntityAffected = true;
                }
                
                if (isEntityAffected && !blockBreakConfig.isAllowBreak()) {
                    // 取消末影龙的爆炸事件
                    event.setCancelled(true);
                    event.blockList().clear();
                    explosionManager.logExplosion("ender-dragon-explode", entityType, true, 
                            String.format("位置: %s, 方块数量: %d",
                                    event.getLocation(), event.blockList().size()));
                }
            }
        }
    }
    
    /**
     * 处理方块破坏事件，特别是末影龙破坏方块的情况
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }
        
        // 检查破坏者是否为末影龙（通过直接破坏或间接破坏）
        org.bukkit.entity.Player player = event.getPlayer();
        if (player == null) {
            // 如果没有玩家，可能是实体破坏
            ExplosionProtectionManager.EntityBlockBreakConfig blockBreakConfig = explosionManager.getEntityBlockBreakConfig();
            
            if (blockBreakConfig.isEnabled() && blockBreakConfig.isApplyToAllEntities() && !blockBreakConfig.isAllowBreak()) {
                // 取消无玩家参与的方块破坏事件（可能是末影龙等实体）
                event.setCancelled(true);
                explosionManager.logExplosion("block-break-no-player", "unknown", true, 
                        String.format("位置: %s, 方块类型: %s",
                                event.getBlock().getLocation(), event.getBlock().getType().name().toLowerCase()));
            }
        }
    }

    /**
     * 处理爆炸触发事件（用于调整爆炸威力）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!explosionManager.isEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        String explosionType = identifyExplosionType(entity);
        ExplosionProtectionManager.ExplosionConfig config = getConfigByType(explosionType);

        if (config != null && config.isEnabled()) {
            // 如果不允许破坏地形，取消爆炸事件
            if (!config.isBreakBlocks()) {
                event.setCancelled(true);
                String entityType = entity.getType().toString().toLowerCase();
                explosionManager.logExplosion("explosion-prime", entityType, true, 
                        String.format("位置: %s, 实体类型: %s",
                                entity.getLocation(), entityType));
                return;
            }

            // 调整爆炸威力
            if (config.getPowerMultiplier() != 1.0) {
                event.setRadius((float) (event.getRadius() * config.getPowerMultiplier()));
            }

            // 限制爆炸范围
            if (config.getMaxRadius() > 0) {
                if (event.getRadius() > config.getMaxRadius()) {
                    event.setRadius((float) config.getMaxRadius());
                }
            }

            // 如果不允许破坏地形，设置Fire为false
            event.setFire(false);
        }
    }

    /**
     * 识别爆炸类型
     */
    private String identifyExplosionType(Entity entity) {
        if (entity instanceof Creeper) {
            return "entity-explosion";
        } else if (entity instanceof TNTPrimed) {
            return "tnt-explosion";
        } else if (entity instanceof EnderCrystal) {
            return "end-crystal-explosion";
        } else if (entity instanceof EnderDragon) {
            return "entity-explosion";
        } else if (entity instanceof LivingEntity) {
            return "entity-explosion";
        } else {
            return "other-explosion";
        }
    }

    /**
     * 识别方块爆炸类型
     */
    private String identifyBlockExplosionType(Block block) {
        Material type = block.getType();
        if (type == Material.TNT || type == Material.TNT_MINECART) {
            return "tnt-explosion";
        } else {
            return "other-explosion";
        }
    }

    /**
     * 根据爆炸类型获取配置
     */
    private ExplosionProtectionManager.ExplosionConfig getConfigByType(String type) {
        switch (type) {
            case "entity-explosion":
                return explosionManager.getEntityExplosionConfig();
            case "tnt-explosion":
                return explosionManager.getTntExplosionConfig();
            case "end-crystal-explosion":
                return explosionManager.getEndCrystalExplosionConfig();
            case "bed-explosion":
                return explosionManager.getBedExplosionConfig();
            case "other-explosion":
                return explosionManager.getOtherExplosionConfig();
            default:
                return null;
        }
    }

    /**
     * 处理生物爆炸
     */
    private void handleEntityExplode(EntityExplodeEvent event, Entity entity) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getEntityExplosionConfig();
        if (!config.isEnabled()) {
            return;
        }

        // 检查是否包含该生物类型
        String entityType = entity.getType().toString().toLowerCase();
        if (!config.getTypes().isEmpty() && !config.getTypes().contains(entityType)) {
            return;
        }

        applyExplosionConfig(event, config, "entity-explosion", entityType);
    }

    /**
     * 处理TNT爆炸
     */
    private void handleTNTExplosion(EntityExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getTntExplosionConfig();
        if (config.isEnabled()) {
            applyExplosionConfig(event, config, "tnt-explosion", "tnt");
        }
    }

    /**
     * 处理TNT方块爆炸
     */
    private void handleTNTBlockExplosion(BlockExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getTntExplosionConfig();
        if (config.isEnabled()) {
            applyBlockExplosionConfig(event, config, "tnt-explosion", "tnt-block");
        }
    }

    /**
     * 处理末影水晶爆炸
     */
    private void handleEndCrystalExplosion(EntityExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getEndCrystalExplosionConfig();
        if (config.isEnabled()) {
            applyExplosionConfig(event, config, "end-crystal-explosion", "end-crystal");
        }
    }

    /**
     * 处理床爆炸
     */
    private void handleBedExplosion(EntityExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getBedExplosionConfig();
        if (config.isEnabled()) {
            applyExplosionConfig(event, config, "bed-explosion", "bed");
        }
    }

    /**
     * 处理其他爆炸
     */
    private void handleOtherExplosion(EntityExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getOtherExplosionConfig();
        if (config.isEnabled()) {
            applyExplosionConfig(event, config, "other-explosion", "other");
        }
    }

    /**
     * 处理其他方块爆炸
     */
    private void handleOtherBlockExplosion(BlockExplodeEvent event) {
        ExplosionProtectionManager.ExplosionConfig config = explosionManager.getOtherExplosionConfig();
        if (config.isEnabled()) {
            applyBlockExplosionConfig(event, config, "other-explosion", "other-block");
        }
    }

    /**
     * 应用爆炸配置
     */
    private void applyExplosionConfig(EntityExplodeEvent event, ExplosionProtectionManager.ExplosionConfig config, String explosionType, String detailedType) {
        boolean blocked = false;

        // 如果不允许破坏地形，清空破坏的方块列表并取消事件
        if (!config.isBreakBlocks()) {
            event.blockList().clear();
            event.setCancelled(true);
            blocked = true;
        }

        // 如果不允许对实体造成伤害，取消事件
        if (!config.isDamageEntities()) {
            event.setCancelled(true);
            blocked = true;
        }

        // 记录日志
        explosionManager.logExplosion(explosionType, detailedType, blocked, 
                String.format("位置: %s, 威力: %.2f, 方块数量: %d",
                        event.getLocation(), event.getYield(), event.blockList().size()));
    }

    /**
     * 应用方块爆炸配置
     */
    private void applyBlockExplosionConfig(BlockExplodeEvent event, ExplosionProtectionManager.ExplosionConfig config, String explosionType, String detailedType) {
        boolean blocked = false;

        // 如果不允许破坏地形，清空破坏的方块列表
        if (!config.isBreakBlocks()) {
            event.blockList().clear();
            blocked = true;
        }

        // 记录日志
        explosionManager.logExplosion(explosionType, detailedType, blocked, 
                String.format("位置: %s, 方块数量: %d",
                        event.getBlock().getLocation(), event.blockList().size()));
    }
}

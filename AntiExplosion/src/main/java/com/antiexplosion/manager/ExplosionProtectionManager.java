package com.antiexplosion.manager;

import com.antiexplosion.AntiExplosion;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.logging.Level;

public class ExplosionProtectionManager {
    private final AntiExplosion plugin;
    private boolean enabled;
    private ExplosionConfig entityExplosionConfig;
    private ExplosionConfig tntExplosionConfig;
    private ExplosionConfig endCrystalExplosionConfig;
    private ExplosionConfig bedExplosionConfig;
    private ExplosionConfig otherExplosionConfig;
    private EntityBlockBreakConfig entityBlockBreakConfig;
    private LoggingConfig loggingConfig;

    public ExplosionProtectionManager(AntiExplosion plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        plugin.reloadConfig();

        // 主开关
        this.enabled = plugin.getConfig().getBoolean("enabled", true);

        // 生物爆炸配置
        ConfigurationSection entityExplosionSection = plugin.getConfig().getConfigurationSection("entity-explosion");
        this.entityExplosionConfig = new ExplosionConfig(entityExplosionSection);

        // 实体破坏方块配置
        ConfigurationSection entityBlockBreakSection = plugin.getConfig().getConfigurationSection("entity-block-break");
        this.entityBlockBreakConfig = new EntityBlockBreakConfig(entityBlockBreakSection);

        // TNT爆炸配置
        ConfigurationSection tntExplosionSection = plugin.getConfig().getConfigurationSection("tnt-explosion");
        this.tntExplosionConfig = new ExplosionConfig(tntExplosionSection);

        // 末影水晶爆炸配置
        ConfigurationSection endCrystalExplosionSection = plugin.getConfig().getConfigurationSection("end-crystal-explosion");
        this.endCrystalExplosionConfig = new ExplosionConfig(endCrystalExplosionSection);

        // 床爆炸配置
        ConfigurationSection bedExplosionSection = plugin.getConfig().getConfigurationSection("bed-explosion");
        this.bedExplosionConfig = new ExplosionConfig(bedExplosionSection);

        // 其他爆炸配置
        ConfigurationSection otherExplosionSection = plugin.getConfig().getConfigurationSection("other-explosion");
        this.otherExplosionConfig = new ExplosionConfig(otherExplosionSection);

        // 日志配置
        ConfigurationSection loggingSection = plugin.getConfig().getConfigurationSection("logging");
        this.loggingConfig = new LoggingConfig(loggingSection);

        plugin.getLogger().info("防爆系统配置已加载！");
    }

    /**
     * 热加载配置
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 保存配置到文件
     */
    public void saveConfig() {
        plugin.getConfig().set("enabled", enabled);
        
        // 保存生物爆炸配置
        plugin.getConfig().set("entity-explosion.enabled", entityExplosionConfig.isEnabled());
        plugin.getConfig().set("entity-explosion.break-blocks", entityExplosionConfig.isBreakBlocks());
        plugin.getConfig().set("entity-explosion.damage-entities", entityExplosionConfig.isDamageEntities());
        
        // 保存实体破坏方块配置
        plugin.getConfig().set("entity-block-break.enabled", entityBlockBreakConfig.isEnabled());
        plugin.getConfig().set("entity-block-break.allow-break", entityBlockBreakConfig.isAllowBreak());
        plugin.getConfig().set("entity-block-break.apply-to-all-entities", entityBlockBreakConfig.isApplyToAllEntities());
        
        // 保存TNT爆炸配置
        plugin.getConfig().set("tnt-explosion.enabled", tntExplosionConfig.isEnabled());
        plugin.getConfig().set("tnt-explosion.break-blocks", tntExplosionConfig.isBreakBlocks());
        plugin.getConfig().set("tnt-explosion.damage-entities", tntExplosionConfig.isDamageEntities());
        
        // 保存末影水晶爆炸配置
        plugin.getConfig().set("end-crystal-explosion.enabled", endCrystalExplosionConfig.isEnabled());
        plugin.getConfig().set("end-crystal-explosion.break-blocks", endCrystalExplosionConfig.isBreakBlocks());
        plugin.getConfig().set("end-crystal-explosion.damage-entities", endCrystalExplosionConfig.isDamageEntities());
        
        // 保存床爆炸配置
        plugin.getConfig().set("bed-explosion.enabled", bedExplosionConfig.isEnabled());
        plugin.getConfig().set("bed-explosion.break-blocks", bedExplosionConfig.isBreakBlocks());
        plugin.getConfig().set("bed-explosion.damage-entities", bedExplosionConfig.isDamageEntities());
        
        // 保存其他爆炸配置
        plugin.getConfig().set("other-explosion.enabled", otherExplosionConfig.isEnabled());
        plugin.getConfig().set("other-explosion.break-blocks", otherExplosionConfig.isBreakBlocks());
        plugin.getConfig().set("other-explosion.damage-entities", otherExplosionConfig.isDamageEntities());
        
        // 保存日志配置
        plugin.getConfig().set("logging.enabled", loggingConfig.isEnabled());
        plugin.getConfig().set("logging.detailed", loggingConfig.isDetailed());
        plugin.getConfig().set("logging.log-blocked", loggingConfig.isLogBlocked());
        plugin.getConfig().set("logging.log-allowed", loggingConfig.isLogAllowed());
        
        plugin.saveConfig();
        plugin.getLogger().info("防爆系统配置已保存！");
    }

    /**
     * 设置防爆系统总开关
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("enabled", enabled);
        plugin.saveConfig();
    }

    /**
     * 设置生物爆炸配置
     * @param enabled 是否启用
     * @param breakBlocks 是否破坏方块
     * @param damageEntities 是否伤害实体
     */
    public void setEntityExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.entityExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        plugin.getConfig().set("entity-explosion.enabled", enabled);
        plugin.getConfig().set("entity-explosion.break-blocks", breakBlocks);
        plugin.getConfig().set("entity-explosion.damage-entities", damageEntities);
        plugin.saveConfig();
    }

    /**
     * 设置实体破坏方块配置
     * @param enabled 是否启用
     * @param allowBreak 是否允许破坏
     * @param applyToAll 是否应用到所有实体
     */
    public void setEntityBlockBreakConfig(boolean enabled, boolean allowBreak, boolean applyToAll) {
        this.entityBlockBreakConfig = new EntityBlockBreakConfig(enabled, allowBreak, applyToAll);
        plugin.getConfig().set("entity-block-break.enabled", enabled);
        plugin.getConfig().set("entity-block-break.allow-break", allowBreak);
        plugin.getConfig().set("entity-block-break.apply-to-all-entities", applyToAll);
        plugin.saveConfig();
    }

    /**
     * 设置TNT爆炸配置
     * @param enabled 是否启用
     * @param breakBlocks 是否破坏方块
     * @param damageEntities 是否伤害实体
     */
    public void setTntExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.tntExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        plugin.getConfig().set("tnt-explosion.enabled", enabled);
        plugin.getConfig().set("tnt-explosion.break-blocks", breakBlocks);
        plugin.getConfig().set("tnt-explosion.damage-entities", damageEntities);
        plugin.saveConfig();
    }

    /**
     * 设置末影水晶爆炸配置
     * @param enabled 是否启用
     * @param breakBlocks 是否破坏方块
     * @param damageEntities 是否伤害实体
     */
    public void setEndCrystalExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.endCrystalExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        plugin.getConfig().set("end-crystal-explosion.enabled", enabled);
        plugin.getConfig().set("end-crystal-explosion.break-blocks", breakBlocks);
        plugin.getConfig().set("end-crystal-explosion.damage-entities", damageEntities);
        plugin.saveConfig();
    }

    /**
     * 设置床爆炸配置
     * @param enabled 是否启用
     * @param breakBlocks 是否破坏方块
     * @param damageEntities 是否伤害实体
     */
    public void setBedExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.bedExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        plugin.getConfig().set("bed-explosion.enabled", enabled);
        plugin.getConfig().set("bed-explosion.break-blocks", breakBlocks);
        plugin.getConfig().set("bed-explosion.damage-entities", damageEntities);
        plugin.saveConfig();
    }

    /**
     * 设置其他爆炸配置
     * @param enabled 是否启用
     * @param breakBlocks 是否破坏方块
     * @param damageEntities 是否伤害实体
     */
    public void setOtherExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.otherExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        plugin.getConfig().set("other-explosion.enabled", enabled);
        plugin.getConfig().set("other-explosion.break-blocks", breakBlocks);
        plugin.getConfig().set("other-explosion.damage-entities", damageEntities);
        plugin.saveConfig();
    }

    /**
     * 设置日志配置
     * @param enabled 是否启用
     * @param detailed 是否详细
     * @param logBlocked 是否记录拦截
     * @param logAllowed 是否记录允许
     */
    public void setLoggingConfig(boolean enabled, boolean detailed, boolean logBlocked, boolean logAllowed) {
        this.loggingConfig = new LoggingConfig(enabled, detailed, logBlocked, logAllowed);
        plugin.getConfig().set("logging.enabled", enabled);
        plugin.getConfig().set("logging.detailed", detailed);
        plugin.getConfig().set("logging.log-blocked", logBlocked);
        plugin.getConfig().set("logging.log-allowed", logAllowed);
        plugin.saveConfig();
    }

    /**
     * 检查防爆系统是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取生物爆炸配置
     */
    public ExplosionConfig getEntityExplosionConfig() {
        return entityExplosionConfig;
    }

    /**
     * 获取TNT爆炸配置
     */
    public ExplosionConfig getTntExplosionConfig() {
        return tntExplosionConfig;
    }

    /**
     * 获取末影水晶爆炸配置
     */
    public ExplosionConfig getEndCrystalExplosionConfig() {
        return endCrystalExplosionConfig;
    }

    /**
     * 获取床爆炸配置
     */
    public ExplosionConfig getBedExplosionConfig() {
        return bedExplosionConfig;
    }

    /**
     * 获取其他爆炸配置
     */
    public ExplosionConfig getOtherExplosionConfig() {
        return otherExplosionConfig;
    }

    /**
     * 获取实体破坏方块配置
     */
    public EntityBlockBreakConfig getEntityBlockBreakConfig() {
        return entityBlockBreakConfig;
    }

    /**
     * 获取日志配置
     */
    public LoggingConfig getLoggingConfig() {
        return loggingConfig;
    }

    /**
     * 记录爆炸日志
     */
    public void logExplosion(String explosionType, String eventType, boolean blocked, String details) {
        if (!loggingConfig.isEnabled()) {
            return;
        }

        if (blocked && !loggingConfig.isLogBlocked()) {
            return;
        }

        if (!blocked && !loggingConfig.isLogAllowed()) {
            return;
        }

        String message = String.format("[防爆系统] 类型: %s, 事件: %s, 处理结果: %s, 详情: %s",
                explosionType, eventType, blocked ? "拦截" : "允许", details);

        Level level = loggingConfig.isDetailed() ? Level.INFO : Level.WARNING;
        plugin.getLogger().log(level, message);
    }

    /**
     * 爆炸配置类
     */
    public static class ExplosionConfig {
        private boolean enabled;
        private List<String> types;
        private boolean breakBlocks;
        private boolean damageEntities;
        private double maxRadius;
        private double powerMultiplier;

        public ExplosionConfig(ConfigurationSection section) {
            if (section == null) {
                this.enabled = false;
                this.types = List.of();
                this.breakBlocks = false;
                this.damageEntities = true;
                this.maxRadius = 0.0;
                this.powerMultiplier = 1.0;
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.types = section.getStringList("types");
            this.breakBlocks = section.getBoolean("break-blocks", false);
            this.damageEntities = section.getBoolean("damage-entities", true);
            this.maxRadius = section.getDouble("max-radius", 0.0);
            this.powerMultiplier = section.getDouble("power-multiplier", 1.0);
        }

        public ExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
            this.enabled = enabled;
            this.types = List.of();
            this.breakBlocks = breakBlocks;
            this.damageEntities = damageEntities;
            this.maxRadius = 0.0;
            this.powerMultiplier = 1.0;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public List<String> getTypes() {
            return types;
        }

        public boolean isBreakBlocks() {
            return breakBlocks;
        }

        public boolean isDamageEntities() {
            return damageEntities;
        }

        public double getMaxRadius() {
            return maxRadius;
        }

        public double getPowerMultiplier() {
            return powerMultiplier;
        }
    }

    /**
     * 日志配置类
     */
    public static class LoggingConfig {
        private boolean enabled;
        private String level;
        private boolean detailed;
        private boolean logBlocked;
        private boolean logAllowed;

        public LoggingConfig(ConfigurationSection section) {
            if (section == null) {
                this.enabled = true;
                this.level = "INFO";
                this.detailed = true;
                this.logBlocked = true;
                this.logAllowed = false;
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.level = section.getString("level", "INFO");
            this.detailed = section.getBoolean("detailed", true);
            this.logBlocked = section.getBoolean("log-blocked", true);
            this.logAllowed = section.getBoolean("log-allowed", false);
        }

        public LoggingConfig(boolean enabled, boolean detailed, boolean logBlocked, boolean logAllowed) {
            this.enabled = enabled;
            this.level = "INFO";
            this.detailed = detailed;
            this.logBlocked = logBlocked;
            this.logAllowed = logAllowed;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getLevel() {
            return level;
        }

        public boolean isDetailed() {
            return detailed;
        }

        public boolean isLogBlocked() {
            return logBlocked;
        }

        public boolean isLogAllowed() {
            return logAllowed;
        }
    }

    /**
     * 实体破坏方块配置类
     */
    public static class EntityBlockBreakConfig {
        private boolean enabled;
        private List<String> types;
        private boolean allowBreak;
        private List<String> allowedBlocks;
        private List<String> blockedBlocks;
        private double maxRange;
        private boolean applyToAllEntities;

        public EntityBlockBreakConfig(ConfigurationSection section) {
            if (section == null) {
                // 默认配置：禁止所有生物破坏所有方块
                this.enabled = true;
                this.types = List.of();
                this.allowBreak = false;
                this.allowedBlocks = List.of();
                this.blockedBlocks = List.of();
                this.maxRange = 0.0;
                this.applyToAllEntities = true;
                return;
            }

            this.enabled = section.getBoolean("enabled", true);
            this.types = section.getStringList("types");
            this.allowBreak = section.getBoolean("allow-break", false);
            this.allowedBlocks = section.getStringList("allowed-blocks");
            this.blockedBlocks = section.getStringList("blocked-blocks");
            this.maxRange = section.getDouble("max-range", 0.0);
            this.applyToAllEntities = section.getBoolean("apply-to-all-entities", true);
        }

        public EntityBlockBreakConfig(boolean enabled, boolean allowBreak, boolean applyToAllEntities) {
            this.enabled = enabled;
            this.types = List.of();
            this.allowBreak = allowBreak;
            this.allowedBlocks = List.of();
            this.blockedBlocks = List.of();
            this.maxRange = 0.0;
            this.applyToAllEntities = applyToAllEntities;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public List<String> getTypes() {
            return types;
        }

        public boolean isAllowBreak() {
            return allowBreak;
        }

        public List<String> getAllowedBlocks() {
            return allowedBlocks;
        }

        public List<String> getBlockedBlocks() {
            return blockedBlocks;
        }

        public double getMaxRange() {
            return maxRange;
        }

        public boolean isApplyToAllEntities() {
            return applyToAllEntities;
        }
    }
}

package com.nekoessentialsx.antiexplosion.manager;

import com.nekoessentialsx.antiexplosion.AntiExplosionModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * 防爆系统配置管理器。配置独立存放在 antiexplosion.yml 中。
 */
public class ExplosionProtectionManager {
    private final AntiExplosionModule module;
    private FileConfiguration config;
    private File configFile;
    private boolean enabled;
    private ExplosionConfig entityExplosionConfig;
    private ExplosionConfig tntExplosionConfig;
    private ExplosionConfig endCrystalExplosionConfig;
    private ExplosionConfig bedExplosionConfig;
    private ExplosionConfig otherExplosionConfig;
    private EntityBlockBreakConfig entityBlockBreakConfig;
    private LoggingConfig loggingConfig;

    public ExplosionProtectionManager(AntiExplosionModule module) {
        this.module = module;
        loadConfig();
    }

    /**
     * 加载配置
     */
    public void loadConfig() {
        configFile = new File(module.getPlugin().getDataFolder(), "antiexplosion.yml");
        if (!configFile.exists()) {
            module.getPlugin().saveResource("antiexplosion.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 主开关
        this.enabled = config.getBoolean("enabled", true);

        // 生物爆炸配置
        ConfigurationSection entityExplosionSection = config.getConfigurationSection("entity-explosion");
        this.entityExplosionConfig = new ExplosionConfig(entityExplosionSection);

        // 实体破坏方块配置
        ConfigurationSection entityBlockBreakSection = config.getConfigurationSection("entity-block-break");
        this.entityBlockBreakConfig = new EntityBlockBreakConfig(entityBlockBreakSection);

        // TNT爆炸配置
        ConfigurationSection tntExplosionSection = config.getConfigurationSection("tnt-explosion");
        this.tntExplosionConfig = new ExplosionConfig(tntExplosionSection);

        // 末影水晶爆炸配置
        ConfigurationSection endCrystalExplosionSection = config.getConfigurationSection("end-crystal-explosion");
        this.endCrystalExplosionConfig = new ExplosionConfig(endCrystalExplosionSection);

        // 床爆炸配置
        ConfigurationSection bedExplosionSection = config.getConfigurationSection("bed-explosion");
        this.bedExplosionConfig = new ExplosionConfig(bedExplosionSection);

        // 其他爆炸配置
        ConfigurationSection otherExplosionSection = config.getConfigurationSection("other-explosion");
        this.otherExplosionConfig = new ExplosionConfig(otherExplosionSection);

        // 日志配置
        ConfigurationSection loggingSection = config.getConfigurationSection("logging");
        this.loggingConfig = new LoggingConfig(loggingSection);

        module.getPlugin().getLogger().info("防爆系统配置已加载！喵~");
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
        config.set("enabled", enabled);

        // 保存生物爆炸配置
        config.set("entity-explosion.enabled", entityExplosionConfig.isEnabled());
        config.set("entity-explosion.break-blocks", entityExplosionConfig.isBreakBlocks());
        config.set("entity-explosion.damage-entities", entityExplosionConfig.isDamageEntities());

        // 保存实体破坏方块配置
        config.set("entity-block-break.enabled", entityBlockBreakConfig.isEnabled());
        config.set("entity-block-break.allow-break", entityBlockBreakConfig.isAllowBreak());
        config.set("entity-block-break.apply-to-all-entities", entityBlockBreakConfig.isApplyToAllEntities());

        // 保存TNT爆炸配置
        config.set("tnt-explosion.enabled", tntExplosionConfig.isEnabled());
        config.set("tnt-explosion.break-blocks", tntExplosionConfig.isBreakBlocks());
        config.set("tnt-explosion.damage-entities", tntExplosionConfig.isDamageEntities());

        // 保存末影水晶爆炸配置
        config.set("end-crystal-explosion.enabled", endCrystalExplosionConfig.isEnabled());
        config.set("end-crystal-explosion.break-blocks", endCrystalExplosionConfig.isBreakBlocks());
        config.set("end-crystal-explosion.damage-entities", endCrystalExplosionConfig.isDamageEntities());

        // 保存床爆炸配置
        config.set("bed-explosion.enabled", bedExplosionConfig.isEnabled());
        config.set("bed-explosion.break-blocks", bedExplosionConfig.isBreakBlocks());
        config.set("bed-explosion.damage-entities", bedExplosionConfig.isDamageEntities());

        // 保存其他爆炸配置
        config.set("other-explosion.enabled", otherExplosionConfig.isEnabled());
        config.set("other-explosion.break-blocks", otherExplosionConfig.isBreakBlocks());
        config.set("other-explosion.damage-entities", otherExplosionConfig.isDamageEntities());

        // 保存日志配置
        config.set("logging.enabled", loggingConfig.isEnabled());
        config.set("logging.detailed", loggingConfig.isDetailed());
        config.set("logging.log-blocked", loggingConfig.isLogBlocked());
        config.set("logging.log-allowed", loggingConfig.isLogAllowed());

        try {
            config.save(configFile);
            module.getPlugin().getLogger().info("防爆系统配置已保存！喵~");
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.WARNING, "保存防爆系统配置失败！", e);
        }
    }

    /**
     * 设置防爆系统总开关
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enabled", enabled);
        saveConfig();
    }

    /**
     * 设置生物爆炸配置
     */
    public void setEntityExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.entityExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        config.set("entity-explosion.enabled", enabled);
        config.set("entity-explosion.break-blocks", breakBlocks);
        config.set("entity-explosion.damage-entities", damageEntities);
        saveConfig();
    }

    /**
     * 设置实体破坏方块配置
     */
    public void setEntityBlockBreakConfig(boolean enabled, boolean allowBreak, boolean applyToAll) {
        this.entityBlockBreakConfig = new EntityBlockBreakConfig(enabled, allowBreak, applyToAll);
        config.set("entity-block-break.enabled", enabled);
        config.set("entity-block-break.allow-break", allowBreak);
        config.set("entity-block-break.apply-to-all-entities", applyToAll);
        saveConfig();
    }

    /**
     * 设置TNT爆炸配置
     */
    public void setTntExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.tntExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        config.set("tnt-explosion.enabled", enabled);
        config.set("tnt-explosion.break-blocks", breakBlocks);
        config.set("tnt-explosion.damage-entities", damageEntities);
        saveConfig();
    }

    /**
     * 设置末影水晶爆炸配置
     */
    public void setEndCrystalExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.endCrystalExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        config.set("end-crystal-explosion.enabled", enabled);
        config.set("end-crystal-explosion.break-blocks", breakBlocks);
        config.set("end-crystal-explosion.damage-entities", damageEntities);
        saveConfig();
    }

    /**
     * 设置床爆炸配置
     */
    public void setBedExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.bedExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        config.set("bed-explosion.enabled", enabled);
        config.set("bed-explosion.break-blocks", breakBlocks);
        config.set("bed-explosion.damage-entities", damageEntities);
        saveConfig();
    }

    /**
     * 设置其他爆炸配置
     */
    public void setOtherExplosionConfig(boolean enabled, boolean breakBlocks, boolean damageEntities) {
        this.otherExplosionConfig = new ExplosionConfig(enabled, breakBlocks, damageEntities);
        config.set("other-explosion.enabled", enabled);
        config.set("other-explosion.break-blocks", breakBlocks);
        config.set("other-explosion.damage-entities", damageEntities);
        saveConfig();
    }

    /**
     * 设置日志配置
     */
    public void setLoggingConfig(boolean enabled, boolean detailed, boolean logBlocked, boolean logAllowed) {
        this.loggingConfig = new LoggingConfig(enabled, detailed, logBlocked, logAllowed);
        config.set("logging.enabled", enabled);
        config.set("logging.detailed", detailed);
        config.set("logging.log-blocked", logBlocked);
        config.set("logging.log-allowed", logAllowed);
        saveConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ExplosionConfig getEntityExplosionConfig() {
        return entityExplosionConfig;
    }

    public ExplosionConfig getTntExplosionConfig() {
        return tntExplosionConfig;
    }

    public ExplosionConfig getEndCrystalExplosionConfig() {
        return endCrystalExplosionConfig;
    }

    public ExplosionConfig getBedExplosionConfig() {
        return bedExplosionConfig;
    }

    public ExplosionConfig getOtherExplosionConfig() {
        return otherExplosionConfig;
    }

    public EntityBlockBreakConfig getEntityBlockBreakConfig() {
        return entityBlockBreakConfig;
    }

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
        module.getPlugin().getLogger().log(level, message);
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
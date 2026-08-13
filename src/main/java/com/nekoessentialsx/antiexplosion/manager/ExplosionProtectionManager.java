package com.nekoessentialsx.antiexplosion.manager;

import com.nekoessentialsx.antiexplosion.AntiExplosionModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * 防爆系统配置管理器。配置独立存放在 antiexplosion.yml 中。
 *
 * <p>支持按维度（世界）单独配置：<code>default</code> 段为默认配置，
 * <code>worlds.&lt;世界名&gt;</code> 段可覆盖指定世界。每个配置档
 * （Profile）包含 11 种爆炸源（苦力怕、凋零、TNT 等）的独立设置。</p>
 */
public class ExplosionProtectionManager {

    /** 全部爆炸源 key，顺序即 GUI 展示顺序 */
    public static final List<String> SOURCE_KEYS = List.of(
            "creeper", "wither", "ender-dragon", "ghast-fireball", "blaze-fireball",
            "wind", "tnt", "end-crystal", "bed", "respawn-anchor", "other");

    private final AntiExplosionModule module;
    private FileConfiguration config;
    private File configFile;
    private boolean enabled;
    private ProfileConfig defaultProfile;
    private final Map<String, ProfileConfig> worldProfiles = new LinkedHashMap<>();
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

        // 默认维度配置
        this.defaultProfile = loadProfile(config.getConfigurationSection("default"), null);

        // 各世界独立配置
        worldProfiles.clear();
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                worldProfiles.put(worldName,
                        loadProfile(worldsSection.getConfigurationSection(worldName), defaultProfile));
            }
        }

        // 日志配置
        this.loggingConfig = new LoggingConfig(config.getConfigurationSection("logging"));

        module.getPlugin().getLogger().info("防爆系统配置已加载！喵~");
    }

    /**
     * 从配置段加载一个维度配置档，缺失的项回退到 fallback（默认配置档）
     */
    private ProfileConfig loadProfile(ConfigurationSection section, ProfileConfig fallback) {
        ProfileConfig profile = new ProfileConfig();
        for (String key : SOURCE_KEYS) {
            SourceConfig fb = fallback != null ? fallback.getSource(key) : null;
            profile.setSource(key, new SourceConfig(
                    section == null ? null : section.getConfigurationSection(key), fb));
        }
        EntityBlockBreakConfig fbBlock = fallback != null ? fallback.getEntityBlockBreakConfig() : null;
        profile.setEntityBlockBreakConfig(new EntityBlockBreakConfig(
                section == null ? null : section.getConfigurationSection("entity-block-break"), fbBlock));
        return profile;
    }

    /**
     * 把一个维度配置档写入配置段
     */
    private void writeProfile(ConfigurationSection section, ProfileConfig profile) {
        for (String key : SOURCE_KEYS) {
            writeSource(section.createSection(key), profile.getSource(key));
        }
        EntityBlockBreakConfig block = profile.getEntityBlockBreakConfig();
        ConfigurationSection blockSection = section.createSection("entity-block-break");
        blockSection.set("enabled", block.isEnabled());
        blockSection.set("types", block.getTypes());
        blockSection.set("allow-break", block.isAllowBreak());
        blockSection.set("allowed-blocks", block.getAllowedBlocks());
        blockSection.set("blocked-blocks", block.getBlockedBlocks());
        blockSection.set("max-range", block.getMaxRange());
        blockSection.set("apply-to-all-entities", block.isApplyToAllEntities());
    }

    private void writeSource(ConfigurationSection section, SourceConfig source) {
        section.set("enabled", source.isEnabled());
        section.set("break-blocks", source.isBreakBlocks());
        section.set("damage-players", source.isDamagePlayers());
        section.set("damage-entities", source.isDamageEntities());
        section.set("power-multiplier", source.getPowerMultiplier());
        section.set("max-radius", source.getMaxRadius());
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

        writeProfile(config.createSection("default"), defaultProfile);

        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection == null) {
            worldsSection = config.createSection("worlds");
        }
        for (Map.Entry<String, ProfileConfig> entry : worldProfiles.entrySet()) {
            writeProfile(worldsSection.createSection(entry.getKey()), entry.getValue());
        }

        config.set("logging.enabled", loggingConfig.isEnabled());
        config.set("logging.level", loggingConfig.getLevel());
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
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enabled", enabled);
        saveConfig();
    }

    /**
     * 获取某个维度的配置档（未单独配置时返回默认档）
     */
    public ProfileConfig getProfile(String worldName) {
        return worldProfiles.getOrDefault(worldName, defaultProfile);
    }

    /**
     * 获取可编辑的配置档（"default" 或指定世界，世界不存在则创建）
     */
    private ProfileConfig getEditableProfile(String worldName) {
        if (worldName == null || worldName.equals("default")) {
            return defaultProfile;
        }
        return worldProfiles.computeIfAbsent(worldName, k -> loadProfile(null, defaultProfile));
    }

    /**
     * 获取某个维度下某爆炸源的配置
     */
    public SourceConfig getSource(String worldName, String sourceKey) {
        return getProfile(worldName).getSource(sourceKey);
    }

    /**
     * 设置某个维度下某爆炸源的配置
     */
    public void setSourceConfig(String worldName, String sourceKey, SourceConfig source) {
        getEditableProfile(worldName).setSource(sourceKey, source);
        saveConfig();
    }

    /**
     * 获取某个维度的实体破坏方块配置
     */
    public EntityBlockBreakConfig getBlockBreakConfig(String worldName) {
        return getProfile(worldName).getEntityBlockBreakConfig();
    }

    /**
     * 设置某个维度的实体破坏方块配置
     */
    public void setBlockBreakConfig(String worldName, boolean enabled, boolean allowBreak, boolean applyToAll) {
        getEditableProfile(worldName).setEntityBlockBreakConfig(
                new EntityBlockBreakConfig(enabled, allowBreak, applyToAll));
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

    public ProfileConfig getDefaultProfile() {
        return defaultProfile;
    }

    /** 已单独配置过的世界名集合 */
    public Set<String> getConfiguredWorlds() {
        return worldProfiles.keySet();
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
     * 维度配置档：包含全部爆炸源 + 实体破坏方块设置
     */
    public static class ProfileConfig {
        private final Map<String, SourceConfig> sources = new LinkedHashMap<>();
        private EntityBlockBreakConfig entityBlockBreakConfig;

        public SourceConfig getSource(String key) {
            return sources.get(key);
        }

        public void setSource(String key, SourceConfig source) {
            sources.put(key, source);
        }

        public EntityBlockBreakConfig getEntityBlockBreakConfig() {
            return entityBlockBreakConfig;
        }

        public void setEntityBlockBreakConfig(EntityBlockBreakConfig config) {
            this.entityBlockBreakConfig = config;
        }
    }

    /**
     * 单个爆炸源的配置。
     *
     * <p>四个开关彼此独立：关闭破坏方块只清理方块列表（不取消事件），
     * 关闭伤害只拦截对应伤害事件，互不影响。</p>
     */
    public static class SourceConfig {
        private boolean enabled;
        private boolean breakBlocks;
        private boolean damagePlayers;
        private boolean damageEntities;
        private double powerMultiplier;
        private double maxRadius;

        public SourceConfig() {
            this.enabled = true;
            this.breakBlocks = false;
            this.damagePlayers = true;
            this.damageEntities = true;
            this.powerMultiplier = 1.0;
            this.maxRadius = 0.0;
        }

        public SourceConfig(ConfigurationSection section, SourceConfig fallback) {
            this();
            if (fallback != null) {
                this.enabled = fallback.isEnabled();
                this.breakBlocks = fallback.isBreakBlocks();
                this.damagePlayers = fallback.isDamagePlayers();
                this.damageEntities = fallback.isDamageEntities();
                this.powerMultiplier = fallback.getPowerMultiplier();
                this.maxRadius = fallback.getMaxRadius();
            }
            if (section != null) {
                this.enabled = section.getBoolean("enabled", this.enabled);
                this.breakBlocks = section.getBoolean("break-blocks", this.breakBlocks);
                this.damagePlayers = section.getBoolean("damage-players", this.damagePlayers);
                this.damageEntities = section.getBoolean("damage-entities", this.damageEntities);
                this.powerMultiplier = section.getDouble("power-multiplier", this.powerMultiplier);
                this.maxRadius = section.getDouble("max-radius", this.maxRadius);
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isBreakBlocks() {
            return breakBlocks;
        }

        public boolean isDamagePlayers() {
            return damagePlayers;
        }

        public boolean isDamageEntities() {
            return damageEntities;
        }

        public double getPowerMultiplier() {
            return powerMultiplier;
        }

        public double getMaxRadius() {
            return maxRadius;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setBreakBlocks(boolean breakBlocks) {
            this.breakBlocks = breakBlocks;
        }

        public void setDamagePlayers(boolean damagePlayers) {
            this.damagePlayers = damagePlayers;
        }

        public void setDamageEntities(boolean damageEntities) {
            this.damageEntities = damageEntities;
        }

        public void setPowerMultiplier(double powerMultiplier) {
            this.powerMultiplier = powerMultiplier;
        }

        public void setMaxRadius(double maxRadius) {
            this.maxRadius = maxRadius;
        }

        /**
         * 该源完全受保护（不允许破坏方块、不允许伤害任何生物）？
         */
        public boolean isFullyProtected() {
            return !breakBlocks && !damagePlayers && !damageEntities;
        }
    }

    /**
     * 实体破坏方块配置类（凋零、末影龙等直接破坏方块）
     */
    public static class EntityBlockBreakConfig {
        private boolean enabled;
        private List<String> types;
        private boolean allowBreak;
        private List<String> allowedBlocks;
        private List<String> blockedBlocks;
        private double maxRange;
        private boolean applyToAllEntities;

        public EntityBlockBreakConfig(ConfigurationSection section, EntityBlockBreakConfig fallback) {
            this.enabled = fallback != null && fallback.isEnabled();
            this.types = fallback != null ? fallback.getTypes() : List.of();
            this.allowBreak = fallback != null && fallback.isAllowBreak();
            this.allowedBlocks = fallback != null ? fallback.getAllowedBlocks() : List.of();
            this.blockedBlocks = fallback != null ? fallback.getBlockedBlocks() : List.of();
            this.maxRange = fallback != null ? fallback.getMaxRange() : 0.0;
            this.applyToAllEntities = fallback == null || fallback.isApplyToAllEntities();

            if (section != null) {
                this.enabled = section.getBoolean("enabled", this.enabled);
                this.types = section.getStringList("types");
                if (this.types.isEmpty()) {
                    this.types = fallback != null ? fallback.getTypes() : List.of();
                }
                this.allowBreak = section.getBoolean("allow-break", this.allowBreak);
                this.allowedBlocks = section.getStringList("allowed-blocks");
                this.blockedBlocks = section.getStringList("blocked-blocks");
                this.maxRange = section.getDouble("max-range", this.maxRange);
                this.applyToAllEntities = section.getBoolean("apply-to-all-entities", this.applyToAllEntities);
            }
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
            this.enabled = true;
            this.level = "INFO";
            this.detailed = true;
            this.logBlocked = true;
            this.logAllowed = false;

            if (section != null) {
                this.enabled = section.getBoolean("enabled", this.enabled);
                this.level = section.getString("level", this.level);
                this.detailed = section.getBoolean("detailed", this.detailed);
                this.logBlocked = section.getBoolean("log-blocked", this.logBlocked);
                this.logAllowed = section.getBoolean("log-allowed", this.logAllowed);
            }
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
}

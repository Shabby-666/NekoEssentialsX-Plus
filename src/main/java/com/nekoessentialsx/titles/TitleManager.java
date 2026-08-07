package com.nekoessentialsx.titles;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TitleManager implements Listener {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private File titlesFile;
    private FileConfiguration titlesConfig;
    private boolean enabled = false;
    private Map<String, String> playerTitles = new HashMap<>();
    private Map<String, Title> titles = new HashMap<>();

    public TitleManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void onEnable() {
        // 加载配置文件
        setupConfig();
        
        // 加载头衔数据
        loadTitles();
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        enabled = true;
        plugin.getLogger().info("头衔管理器已启用！喵~");
    }

    public void onDisable() {
        enabled = false;
        plugin.getLogger().info("头衔管理器已禁用！喵~");
    }

    public void reload() {
        setupConfig();
        loadTitles();
        plugin.getLogger().info("头衔配置已重载！喵~");
    }

    private void setupConfig() {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        titlesFile = new File(plugin.getDataFolder(), "titles.yml");
        
        try {
            // 从JAR文件中保存默认配置到数据文件夹
            // 只有当文件不存在时才保存，避免产生警告
            if (!titlesFile.exists()) {
                plugin.saveResource("titles.yml", false);
            }
            
            // 现在使用标准方式加载配置文件
            titlesConfig = YamlConfiguration.loadConfiguration(titlesFile);
            plugin.getLogger().info("成功加载titles.yml配置文件！喵~");
        } catch (Exception e) {
            plugin.getLogger().severe("加载titles.yml配置文件失败: " + e.getMessage());
            // 创建一个基本配置作为最后回退
            titlesConfig = new YamlConfiguration();
            titlesConfig.set("enabled", true);
            
            // 创建一个简单的头衔配置
            java.util.Map<String, Object> titles = new java.util.HashMap<>();
            java.util.Map<String, Object> newbie = new java.util.HashMap<>();
            newbie.put("name", "新手");
            newbie.put("prefix", "[新手] ");
            newbie.put("suffix", "");
            newbie.put("permission", "nekoessentialsx.titles.newbie");
            newbie.put("priority", 1);
            newbie.put("enabled", true);
            titles.put("newbie", newbie);
            titlesConfig.set("titles", titles);
        }
    }

    private void loadTitles() {
        titles.clear();
        // 从配置中加载头衔
        if (titlesConfig.contains("titles")) {
            for (String titleId : titlesConfig.getConfigurationSection("titles").getKeys(false)) {
                Title title = new Title(titleId, titlesConfig);
                titles.put(titleId, title);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getName();
        
        // 自动给玩家添加新手头衔到仓库
        if (!databaseManager.hasTitle(playerId, "newbie")) {
            databaseManager.addTitleToInventory(playerId, "newbie", false);
        }
        
        // 从数据库加载玩家的头衔
        String titleId = getPlayerTitle(playerId);
        if (titleId != null) {
            // 检查是否为系统头衔或自定义头衔
            boolean isSystemTitle = titles.containsKey(titleId);
            boolean isCustomTitle = databaseManager.getCustomTitle(titleId) != null;
            
            if (isSystemTitle || isCustomTitle) {
                updatePlayerTitle(player, titleId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getName();
        
        // 保存玩家的头衔到数据库
        savePlayerTitle(playerId, getPlayerTitle(playerId));
    }

    public void updatePlayerTitle(Player player, String titleId) {
        if (!enabled) {
            return;
        }
        
        // 移除玩家当前的计分板团队
        removePlayerFromTeams(player);
        
        String playerId = player.getName();
        if (titleId == null) {
            // 如果头衔为空，重置玩家名称
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            setPlayerTitle(playerId, null);
            return;
        }
        
        String prefix = "";
        
        // 检查是否为系统头衔
        if (titles.containsKey(titleId)) {
            // 系统头衔
            Title title = titles.get(titleId);
            prefix = title.getPrefix();
        } else {
            // 自定义头衔，从数据库获取
            Object[] customTitleData = databaseManager.getCustomTitle(titleId);
            if (customTitleData != null) {
                // 自定义头衔存在
                prefix = (String) customTitleData[1];
            } else {
                // 头衔不存在，重置玩家名称
                player.setDisplayName(player.getName());
                player.setPlayerListName(player.getName());
                setPlayerTitle(playerId, null);
                return;
            }
        }
        
        // 更新玩家的显示名称
        String displayName = prefix + player.getName();
        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
        
        // 更新计分板团队
        updateScoreboardTeam(player, prefix);
        
        // 保存玩家的头衔到内存和数据库
        setPlayerTitle(playerId, titleId);
    }
    
    /**
     * 从所有计分板团队中移除玩家
     * @param player 玩家
     */
    private void removePlayerFromTeams(Player player) {
        org.bukkit.scoreboard.Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    private void updateScoreboardTeam(Player player, String prefix) {
        // 简单的计分板团队更新逻辑
        String teamName = "title_" + prefix.replaceAll("[^a-zA-Z0-9]", "");
        
        org.bukkit.scoreboard.Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);
        
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        team.setPrefix(prefix);
        team.addEntry(player.getName());
    }

    public String getPlayerTitle(String playerId) {
        // 先从内存中获取
        if (playerTitles.containsKey(playerId)) {
            return playerTitles.get(playerId);
        }
        
        // 如果内存中没有，从数据库中获取
        String titleId = databaseManager.getPlayerTitle(playerId);
        if (titleId != null) {
            playerTitles.put(playerId, titleId);
        }
        
        return titleId;
    }

    public void setPlayerTitle(String playerId, String titleId) {
        // 保存到内存
        if (titleId == null) {
            playerTitles.remove(playerId);
            // 如果titleId为null，删除记录而不是插入null
            databaseManager.removePlayerTitle(playerId);
        } else {
            // 检查是否为系统头衔或自定义头衔
            boolean isSystemTitle = titles.containsKey(titleId);
            boolean isCustomTitle = databaseManager.getCustomTitle(titleId) != null;
            
            if (isSystemTitle || isCustomTitle) {
                playerTitles.put(playerId, titleId);
                // 保存到数据库
                databaseManager.savePlayerTitle(playerId, titleId);
            } else {
                // 头衔不存在，删除记录
                playerTitles.remove(playerId);
                databaseManager.removePlayerTitle(playerId);
            }
        }
    }

    public void savePlayerTitle(String playerId, String titleId) {
        if (titleId != null) {
            databaseManager.savePlayerTitle(playerId, titleId);
        }
    }

    public void clearPlayerTitle(String playerId) {
        // 从内存中移除
        playerTitles.remove(playerId);
        // 从数据库中移除
        databaseManager.removePlayerTitle(playerId);
        
        // 更新在线玩家的显示名称和计分板团队
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            updatePlayerTitle(player, null);
        }
    }

    public Map<String, Title> getTitles() {
        return titles;
    }

    public Title getTitle(String titleId) {
        return titles.get(titleId);
    }

    public boolean isEnabled() {
        return enabled && titlesConfig.getBoolean("enabled", true);
    }

    public FileConfiguration getConfig() {
        return titlesConfig;
    }

    public NekoEssentialX getPlugin() {
        return plugin;
    }
    
    // ====================== 管理员功能方法 ======================
    
    /**
     * 创建新的系统称号
     * @param titleId 称号ID
     * @param name 称号名称
     * @param prefix 称号前缀
     * @param suffix 称号后缀
     * @param permission 所需权限
     * @param priority 优先级
     * @param enabled 是否启用
     */
    public void createTitle(String titleId, String name, String prefix, String suffix, String permission, int priority, boolean enabled) {
        // 更新配置文件
        titlesConfig.set("titles." + titleId + ".name", name);
        titlesConfig.set("titles." + titleId + ".prefix", prefix);
        titlesConfig.set("titles." + titleId + ".suffix", suffix);
        titlesConfig.set("titles." + titleId + ".permission", permission);
        titlesConfig.set("titles." + titleId + ".priority", priority);
        titlesConfig.set("titles." + titleId + ".enabled", enabled);
        
        // 保存配置文件
        saveTitlesConfig();
        
        // 重新加载称号数据
        loadTitles();
    }
    
    /**
     * 编辑系统称号
     * @param titleId 称号ID
     * @param name 称号名称
     * @param prefix 称号前缀
     * @param suffix 称号后缀
     * @param permission 所需权限
     * @param priority 优先级
     * @param enabled 是否启用
     */
    public void editTitle(String titleId, String name, String prefix, String suffix, String permission, int priority, boolean enabled) {
        // 更新配置文件
        titlesConfig.set("titles." + titleId + ".name", name);
        titlesConfig.set("titles." + titleId + ".prefix", prefix);
        titlesConfig.set("titles." + titleId + ".suffix", suffix);
        titlesConfig.set("titles." + titleId + ".permission", permission);
        titlesConfig.set("titles." + titleId + ".priority", priority);
        titlesConfig.set("titles." + titleId + ".enabled", enabled);
        
        // 保存配置文件
        saveTitlesConfig();
        
        // 重新加载称号数据
        loadTitles();
    }
    
    /**
     * 删除系统称号
     * @param titleId 称号ID
     */
    public void deleteTitle(String titleId) {
        // 从配置文件中移除称号
        titlesConfig.set("titles." + titleId, null);
        
        // 保存配置文件
        saveTitlesConfig();
        
        // 重新加载称号数据
        loadTitles();
    }
    
    /**
     * 启用/禁用系统称号
     * @param titleId 称号ID
     * @param enabled 是否启用
     */
    public void toggleTitleEnabled(String titleId, boolean enabled) {
        // 更新配置文件
        titlesConfig.set("titles." + titleId + ".enabled", enabled);
        
        // 保存配置文件
        saveTitlesConfig();
        
        // 重新加载称号数据
        loadTitles();
    }
    
    /**
     * 保存称号配置文件
     */
    private void saveTitlesConfig() {
        try {
            titlesConfig.save(titlesFile);
            plugin.getLogger().info("称号配置已保存！喵~");
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("保存称号配置失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 头衔类
    public static class Title {
        private final String id;
        private final String name;
        private final String prefix;
        private final String suffix;
        private final String permission;
        private final int priority;
        private final boolean enabled;

        public Title(String id, FileConfiguration config) {
            this.id = id;
            this.name = config.getString("titles." + id + ".name", id);
            this.prefix = config.getString("titles." + id + ".prefix", "");
            this.suffix = config.getString("titles." + id + ".suffix", "");
            this.permission = config.getString("titles." + id + ".permission", "nekoessentialsx.titles." + id);
            this.priority = config.getInt("titles." + id + ".priority", 0);
            this.enabled = config.getBoolean("titles." + id + ".enabled", true);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getPermission() {
            return permission;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
package com.nekoessentialsx.database;

import com.nekoessentialsx.NekoEssentialX;

import java.io.File;
import java.sql.*;
import java.util.Date;
import java.util.UUID;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final NekoEssentialX plugin;
    private Connection connection;

    private DatabaseManager(NekoEssentialX plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取数据库管理器实例
     * @param plugin 插件实例
     * @return DatabaseManager实例
     */
    public static synchronized DatabaseManager getInstance(NekoEssentialX plugin) {
        if (instance == null) {
            instance = new DatabaseManager(plugin);
        }
        return instance;
    }

    /**
     * 初始化数据库连接和表结构
     */
    public void initialize() {
        try {
            // 加载SQLite驱动
            Class.forName("org.sqlite.JDBC");
            
            // 确保数据文件夹存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // 创建数据库文件
            File dbFile = new File(plugin.getDataFolder(), "nekoessentialsx.db");
            
            // 连接数据库
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            // 创建表结构
            createTables();
            
            plugin.getLogger().info("数据库连接成功！喵~");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("数据库连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，创建表失败！喵~");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            // 创建玩家头衔表 - 使用player_id（游戏ID）作为主键
            String createPlayerTitlesTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_titles (" +
                    "    player_id TEXT PRIMARY KEY," +
                    "    title_id TEXT NOT NULL," +
                    "    updated_at INTEGER DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            
            // 创建玩家头衔仓库表
            String createPlayerTitleInventoryTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_title_inventory (" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    player_id TEXT NOT NULL," +
                    "    title_id TEXT NOT NULL," +
                    "    acquired_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                    "    is_custom INTEGER DEFAULT 0," +
                    "    UNIQUE(player_id, title_id)" +
                    ");";
            
            // 创建自定义头衔表
            String createCustomTitlesTable = "" +
                    "CREATE TABLE IF NOT EXISTS custom_titles (" +
                    "    title_id TEXT PRIMARY KEY," +
                    "    player_id TEXT NOT NULL," +
                    "    name TEXT NOT NULL," +
                    "    prefix TEXT NOT NULL," +
                    "    suffix TEXT DEFAULT ''," +
                    "    created_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                    "    is_enabled INTEGER DEFAULT 1" +
                    ");";
            
            // 创建交易记录表
            String createTransactionLogTable = "" +
                    "CREATE TABLE IF NOT EXISTS transaction_log (" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    player_id TEXT NOT NULL," +
                    "    type TEXT NOT NULL," +
                    "    amount INTEGER NOT NULL," +
                    "    description TEXT NOT NULL," +
                    "    created_at INTEGER DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            
            // 创建玩家经济表
            String createPlayerEconomyTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_economy (" +
                    "    player_id TEXT PRIMARY KEY," +
                    "    balance REAL DEFAULT 0.0," +
                    "    updated_at INTEGER DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            
            // 创建玩家新手礼包表
            String createPlayerNewbieGiftTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_newbie_gift (" +
                    "    player_id TEXT PRIMARY KEY," +
                    "    claimed INTEGER DEFAULT 0," +
                    "    claimed_at INTEGER DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            
            // 创建玩家登录表
            String createPlayerLoginTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_login (" +
                    "    player_id TEXT PRIMARY KEY," +
                    "    last_login INTEGER DEFAULT CURRENT_TIMESTAMP," +
                    "    login_days INTEGER DEFAULT 0," +
                    "    last_check_in INTEGER DEFAULT NULL" +
                    ");";
            
            // 创建玩家家表
            String createPlayerHomesTable = "" +
                    "CREATE TABLE IF NOT EXISTS player_homes (" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    player_id TEXT NOT NULL," +
                    "    home_name TEXT NOT NULL DEFAULT 'default'," +
                    "    world TEXT NOT NULL," +
                    "    x REAL NOT NULL," +
                    "    y REAL NOT NULL," +
                    "    z REAL NOT NULL," +
                    "    yaw REAL NOT NULL DEFAULT 0.0," +
                    "    pitch REAL NOT NULL DEFAULT 0.0," +
                    "    created_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                    "    UNIQUE(player_id, home_name)" +
                    ");";
            
            stmt.execute(createPlayerTitlesTable);
            stmt.execute(createPlayerTitleInventoryTable);
            stmt.execute(createCustomTitlesTable);
            stmt.execute(createTransactionLogTable);
            stmt.execute(createPlayerEconomyTable);
            stmt.execute(createPlayerNewbieGiftTable);
            stmt.execute(createPlayerLoginTable);
            stmt.execute(createPlayerHomesTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据库表失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存玩家头衔
     * @param playerId 玩家游戏ID
     * @param titleId 头衔ID
     */
    public void savePlayerTitle(String playerId, String titleId) {
        String sql = "INSERT OR REPLACE INTO player_titles (player_id, title_id, updated_at) VALUES (?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, titleId);
            pstmt.setLong(3, System.currentTimeMillis() / 1000);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家头衔失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家头衔
     * @param playerId 玩家游戏ID
     * @return 头衔ID，如果不存在则返回null
     */
    public String getPlayerTitle(String playerId) {
        String sql = "SELECT title_id FROM player_titles WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return null;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("title_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家头衔失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * 删除玩家头衔
     * @param playerId 玩家游戏ID
     */
    public void removePlayerTitle(String playerId) {
        String sql = "DELETE FROM player_titles WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家头衔失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ====================== 玩家头衔仓库相关方法 ======================
    
    /**
     * 添加头衔到玩家仓库
     * @param playerId 玩家游戏ID
     * @param titleId 头衔ID
     * @param isCustom 是否为自定义头衔
     * @return 是否添加成功
     */
    public boolean addTitleToInventory(String playerId, String titleId, boolean isCustom) {
        String sql = "INSERT OR IGNORE INTO player_title_inventory (player_id, title_id, is_custom) VALUES (?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, titleId);
            pstmt.setInt(3, isCustom ? 1 : 0);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("添加头衔到仓库失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从玩家仓库移除头衔
     * @param playerId 玩家游戏ID
     * @param titleId 头衔ID
     * @return 是否移除成功
     */
    public boolean removeTitleFromInventory(String playerId, String titleId) {
        String sql = "DELETE FROM player_title_inventory WHERE player_id = ? AND title_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, titleId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("从仓库移除头衔失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查玩家是否拥有某个头衔
     * @param playerId 玩家游戏ID
     * @param titleId 头衔ID
     * @return 是否拥有
     */
    public boolean hasTitle(String playerId, String titleId) {
        String sql = "SELECT 1 FROM player_title_inventory WHERE player_id = ? AND title_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, titleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查玩家头衔失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ====================== 自定义头衔相关方法 ======================
    
    /**
     * 保存自定义头衔
     * @param titleId 头衔ID
     * @param playerId 玩家游戏ID
     * @param name 头衔名称
     * @param prefix 头衔前缀
     * @param suffix 头衔后缀
     * @return 是否保存成功
     */
    public boolean saveCustomTitle(String titleId, String playerId, String name, String prefix, String suffix) {
        String sql = "INSERT OR REPLACE INTO custom_titles (title_id, player_id, name, prefix, suffix) VALUES (?, ?, ?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titleId);
            pstmt.setString(2, playerId);
            pstmt.setString(3, name);
            pstmt.setString(4, prefix);
            pstmt.setString(5, suffix);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("保存自定义头衔失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取自定义头衔
     * @param titleId 头衔ID
     * @return 自定义头衔数据，格式：[name, prefix, suffix, player_id, is_enabled]
     */
    public Object[] getCustomTitle(String titleId) {
        String sql = "SELECT name, prefix, suffix, player_id, is_enabled FROM custom_titles WHERE title_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return null;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[] {
                        rs.getString("name"),
                        rs.getString("prefix"),
                        rs.getString("suffix"),
                        rs.getString("player_id"),
                        rs.getInt("is_enabled") == 1
                    };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取自定义头衔失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 删除自定义头衔
     * @param titleId 头衔ID
     * @return 是否删除成功
     */
    public boolean deleteCustomTitle(String titleId) {
        String sql = "DELETE FROM custom_titles WHERE title_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titleId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除自定义头衔失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家的所有自定义头衔
     * @param playerId 玩家游戏ID
     * @return 自定义头衔ID列表
     */
    public java.util.List<String> getPlayerCustomTitles(String playerId) {
        java.util.List<String> titleIds = new java.util.ArrayList<>();
        String sql = "SELECT title_id FROM custom_titles WHERE player_id = ? AND is_enabled = 1;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return titleIds;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    titleIds.add(rs.getString("title_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家自定义头衔失败：" + e.getMessage());
            e.printStackTrace();
        }
        return titleIds;
    }    

    // ====================== 新手礼包相关方法 ======================
    
    /**
     * 检查玩家是否已领取新手礼包
     * @param playerId 玩家游戏ID
     * @return 是否已领取
     */
    public boolean hasClaimedNewbieGift(String playerId) {
        String sql = "SELECT claimed FROM player_newbie_gift WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("claimed") == 1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查玩家新手礼包领取状态失败：" + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 标记玩家已领取新手礼包
     * @param playerId 玩家游戏ID
     * @return 是否标记成功
     */
    public boolean markNewbieGiftClaimed(String playerId) {
        String sql = "INSERT OR REPLACE INTO player_newbie_gift (player_id, claimed, claimed_at) VALUES (?, 1, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setLong(2, System.currentTimeMillis() / 1000);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("标记玩家新手礼包领取状态失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ====================== 每日登录相关方法 ======================
    
    /**
     * 获取玩家上次登录时间
     * @param playerId 玩家游戏ID
     * @return 上次登录时间
     */
    public Date getPlayerLastLogin(String playerId) {
        String sql = "SELECT last_login FROM player_login WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return null;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long lastLoginTimestamp = rs.getLong("last_login") * 1000;
                    return new Date(lastLoginTimestamp);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家上次登录时间失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 更新玩家上次登录时间
     * @param playerId 玩家游戏ID
     * @param loginDate 登录时间
     * @return 是否更新成功
     */
    public boolean updatePlayerLastLogin(String playerId, Date loginDate) {
        long loginTimestamp = loginDate.getTime() / 1000;
        // 使用UPDATE语句，只更新last_login字段，保留其他字段的值
        String sql = "UPDATE player_login SET last_login = ? WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, loginTimestamp);
            pstmt.setString(2, playerId);
            int rowsAffected = pstmt.executeUpdate();
            
            // 如果没有更新任何行，说明记录不存在，使用INSERT语句
            if (rowsAffected == 0) {
                sql = "INSERT INTO player_login (player_id, last_login) VALUES (?, ?);";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                    insertStmt.setString(1, playerId);
                    insertStmt.setLong(2, loginTimestamp);
                    rowsAffected = insertStmt.executeUpdate();
                }
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新玩家上次登录时间失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家累计登录天数
     * @param playerId 玩家游戏ID
     * @return 累计登录天数
     */
    public int getPlayerLoginDays(String playerId) {
        String sql = "SELECT login_days FROM player_login WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return 0;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("login_days");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家累计登录天数失败：" + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 设置玩家累计登录天数
     * @param playerId 玩家游戏ID
     * @param days 累计登录天数
     * @return 是否设置成功
     */
    public boolean setPlayerLoginDays(String playerId, int days) {
        // 使用UPDATE语句，只更新login_days字段，保留其他字段的值
        String sql = "UPDATE player_login SET login_days = ? WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            pstmt.setString(2, playerId);
            int rowsAffected = pstmt.executeUpdate();
            
            // 如果没有更新任何行，说明记录不存在，使用INSERT语句插入完整记录
            if (rowsAffected == 0) {
                // 先查询是否存在记录，避免主键冲突
                String checkSql = "SELECT 1 FROM player_login WHERE player_id = ?;";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, playerId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            // 记录不存在，插入完整记录
                            sql = "INSERT INTO player_login (player_id, login_days) VALUES (?, ?);";
                            try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                                insertStmt.setString(1, playerId);
                                insertStmt.setInt(2, days);
                                rowsAffected = insertStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("设置玩家累计登录天数失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家上次签到时间
     * @param playerId 玩家游戏ID
     * @return 上次签到时间
     */
    public Date getPlayerLastCheckIn(String playerId) {
        String sql = "SELECT last_check_in FROM player_login WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return null;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long lastCheckInTimestamp = rs.getLong("last_check_in");
                    // 检查获取的值是否为NULL
                    if (!rs.wasNull() && lastCheckInTimestamp > 0) {
                        return new Date(lastCheckInTimestamp * 1000);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家上次签到时间失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 更新玩家上次签到时间
     * @param playerId 玩家游戏ID
     * @param checkInDate 签到时间
     * @return 是否更新成功
     */
    public boolean updatePlayerLastCheckIn(String playerId, Date checkInDate) {
        long checkInTimestamp = checkInDate.getTime() / 1000;
        // 使用UPDATE语句，只更新last_check_in字段，保留其他字段的值
        String sql = "UPDATE player_login SET last_check_in = ? WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, checkInTimestamp);
            pstmt.setString(2, playerId);
            int rowsAffected = pstmt.executeUpdate();
            
            // 如果没有更新任何行，说明记录不存在，使用INSERT语句插入完整记录
            if (rowsAffected == 0) {
                // 先查询是否存在记录，避免主键冲突
                String checkSql = "SELECT 1 FROM player_login WHERE player_id = ?;";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, playerId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            // 记录不存在，插入完整记录，保留默认值
                            sql = "INSERT INTO player_login (player_id, last_check_in) VALUES (?, ?);";
                            try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                                insertStmt.setString(1, playerId);
                                insertStmt.setLong(2, checkInTimestamp);
                                rowsAffected = insertStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新玩家上次签到时间失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ====================== 交易记录相关方法 ======================
    
    /**
     * 添加交易记录
     * @param playerId 玩家游戏ID
     * @param type 交易类型
     * @param amount 交易金额
     * @param description 交易描述
     * @return 是否添加成功
     */
    public boolean addTransactionLog(String playerId, String type, int amount, String description) {
        String sql = "INSERT INTO transaction_log (player_id, type, amount, description) VALUES (?, ?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, type);
            pstmt.setInt(3, amount);
            pstmt.setString(4, description);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("添加交易记录失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // ====================== 玩家经济相关方法 ======================
    
    /**
     * 获取玩家余额
     * @param playerId 玩家游戏ID
     * @return 玩家余额
     */
    public double getPlayerBalance(String playerId) {
        String sql = "SELECT balance FROM player_economy WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return 0.0;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
            
            // 如果玩家不存在，返回默认余额0.0
            return 0.0;
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家余额失败：" + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * 设置玩家余额
     * @param playerId 玩家游戏ID
     * @param balance 要设置的余额
     * @return 是否设置成功
     */
    public boolean setPlayerBalance(String playerId, double balance) {
        String sql = "INSERT OR REPLACE INTO player_economy (player_id, balance, updated_at) VALUES (?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setDouble(2, balance);
            pstmt.setLong(3, System.currentTimeMillis() / 1000);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("设置玩家余额失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 增加玩家余额
     * @param playerId 玩家游戏ID
     * @param amount 要增加的金额
     * @return 是否增加成功
     */
    public boolean addPlayerBalance(String playerId, double amount) {
        double currentBalance = getPlayerBalance(playerId);
        return setPlayerBalance(playerId, currentBalance + amount);
    }
    
    /**
     * 减少玩家余额
     * @param playerId 玩家游戏ID
     * @param amount 要减少的金额
     * @return 是否减少成功
     */
    public boolean subtractPlayerBalance(String playerId, double amount) {
        double currentBalance = getPlayerBalance(playerId);
        if (currentBalance < amount) {
            return false;
        }
        return setPlayerBalance(playerId, currentBalance - amount);
    }
    
    /**
     * 检查玩家是否有足够的余额
     * @param playerId 玩家游戏ID
     * @param amount 需要的金额
     * @return 是否有足够的余额
     */
    public boolean hasSufficientBalance(String playerId, double amount) {
        return getPlayerBalance(playerId) >= amount;
    }

    // ====================== 玩家家相关方法 ======================
    
    /**
     * 保存玩家家
     * @param playerId 玩家游戏ID
     * @param homeName 家的名称
     * @param world 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yaw 偏航角
     * @param pitch 俯仰角
     * @return 是否保存成功
     */
    public boolean savePlayerHome(String playerId, String homeName, String world, double x, double y, double z, float yaw, float pitch) {
        String sql = "INSERT OR REPLACE INTO player_homes (player_id, home_name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, homeName);
            pstmt.setString(3, world);
            pstmt.setDouble(4, x);
            pstmt.setDouble(5, y);
            pstmt.setDouble(6, z);
            pstmt.setFloat(7, yaw);
            pstmt.setFloat(8, pitch);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家家失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家家
     * @param playerId 玩家游戏ID
     * @param homeName 家的名称
     * @return 家的位置信息，格式：[world, x, y, z, yaw, pitch]，如果不存在则返回null
     */
    public Object[] getPlayerHome(String playerId, String homeName) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM player_homes WHERE player_id = ? AND home_name = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return null;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, homeName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[] {
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家家失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 删除玩家家
     * @param playerId 玩家游戏ID
     * @param homeName 家的名称
     * @return 是否删除成功
     */
    public boolean deletePlayerHome(String playerId, String homeName) {
        String sql = "DELETE FROM player_homes WHERE player_id = ? AND home_name = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, homeName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家家失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家所有家的名称
     * @param playerId 玩家游戏ID
     * @return 家的名称列表
     */
    public java.util.List<String> getPlayerHomeNames(String playerId) {
        java.util.List<String> homeNames = new java.util.ArrayList<>();
        String sql = "SELECT home_name FROM player_homes WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return homeNames;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homeNames.add(rs.getString("home_name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家家列表失败：" + e.getMessage());
            e.printStackTrace();
        }
        return homeNames;
    }
    
    /**
     * 检查玩家是否有家
     * @param playerId 玩家游戏ID
     * @param homeName 家的名称
     * @return 是否有该名称的家
     */
    public boolean hasPlayerHome(String playerId, String homeName) {
        String sql = "SELECT 1 FROM player_homes WHERE player_id = ? AND home_name = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return false;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            pstmt.setString(2, homeName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查玩家家失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取玩家家的数量
     * @param playerId 玩家游戏ID
     * @return 家的数量
     */
    public int getPlayerHomeCount(String playerId) {
        String sql = "SELECT COUNT(*) FROM player_homes WHERE player_id = ?;";
        
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().severe("无法获取数据库连接，操作失败！喵~");
            return 0;
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家家数量失败：" + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭！喵~");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接，自动检测并重新连接
     * @return Connection对象，如果无法连接则返回null
     */
    public synchronized Connection getConnection() {
        try {
            // 检查连接是否为空或已关闭
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().info("数据库连接已关闭，正在重新连接...喵~");
                
                // 重新连接数据库
                File dbFile = new File(plugin.getDataFolder(), "nekoessentialsx.db");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                plugin.getLogger().info("数据库重新连接成功！喵~");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("重新连接数据库失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return connection;
    }
}
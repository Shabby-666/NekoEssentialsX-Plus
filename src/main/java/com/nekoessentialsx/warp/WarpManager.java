package com.nekoessentialsx.warp;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarpManager {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;

    public WarpManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        createWarpTable();
    }

    /**
     * 创建传送点表
     */
    private void createWarpTable() {
        String createWarpTableSQL = "CREATE TABLE IF NOT EXISTS warps (" +
                "warp_name TEXT PRIMARY KEY," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL DEFAULT 0.0," +
                "pitch REAL NOT NULL DEFAULT 0.0," +
                "created_by TEXT NOT NULL," +
                "created_at INTEGER DEFAULT CURRENT_TIMESTAMP" +
                ");";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(createWarpTableSQL);
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("创建传送点表失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建传送点
     * @param warpName 传送点名称
     * @param location 传送点位置
     * @param creator 创建者名称
     * @return 是否创建成功
     */
    public boolean createWarp(String warpName, Location location, String creator) {
        String sql = "INSERT OR REPLACE INTO warps (warp_name, world, x, y, z, yaw, pitch, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warpName);
            pstmt.setString(2, location.getWorld().getName());
            pstmt.setDouble(3, location.getX());
            pstmt.setDouble(4, location.getY());
            pstmt.setDouble(5, location.getZ());
            pstmt.setFloat(6, location.getYaw());
            pstmt.setFloat(7, location.getPitch());
            pstmt.setString(8, creator);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("创建传送点失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除传送点
     * @param warpName 传送点名称
     * @return 是否删除成功
     */
    public boolean deleteWarp(String warpName) {
        String sql = "DELETE FROM warps WHERE warp_name = ?;";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warpName);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("删除传送点失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取传送点位置
     * @param warpName 传送点名称
     * @return 传送点位置，如果不存在则返回null
     */
    public Location getWarpLocation(String warpName) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM warps WHERE warp_name = ?;";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warpName);
            
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        return null;
                    }
                    
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    
                    return new Location(world, x, y, z, yaw, pitch);
                }
            }
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("获取传送点位置失败：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检查传送点是否存在
     * @param warpName 传送点名称
     * @return 是否存在
     */
    public boolean warpExists(String warpName) {
        String sql = "SELECT 1 FROM warps WHERE warp_name = ?;";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, warpName);
            
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("检查传送点是否存在失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取所有传送点名称
     * @return 传送点名称列表
     */
    public List<String> getWarpNames() {
        List<String> warpNames = new ArrayList<>();
        String sql = "SELECT warp_name FROM warps ORDER BY warp_name;";
        
        try (java.sql.Connection conn = databaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                warpNames.add(rs.getString("warp_name"));
            }
        } catch (java.sql.SQLException e) {
            plugin.getLogger().severe("获取传送点列表失败：" + e.getMessage());
            e.printStackTrace();
        }
        return warpNames;
    }

    /**
     * 传送到指定传送点
     * @param player 玩家
     * @param warpName 传送点名称
     * @return 是否传送成功
     */
    public boolean teleportToWarp(Player player, String warpName) {
        Location warpLocation = getWarpLocation(warpName);
        if (warpLocation == null) {
            return false;
        }
        
        player.teleport(warpLocation);
        return true;
    }
}
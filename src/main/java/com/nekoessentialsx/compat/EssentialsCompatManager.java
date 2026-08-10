package com.nekoessentialsx.compat;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EssentialsCompatManager {
    private final NekoEssentialX plugin;
    private final File essentialsFolder;

    public EssentialsCompatManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials");
    }

    /**
     * 检测 EssentialsX 配置文件夹，若存在则自动导入工具包、传送点与玩家家数据
     */
    public void checkCompat() {
        if (!essentialsFolder.isDirectory()) {
            return;
        }
        
        plugin.getLogger().info("==============================================");
        plugin.getLogger().info("检测到EssentialsX配置文件夹，使用兼容模式！");
        int kits = importKits();
        int warps = importWarps();
        int homes = importHomes();
        plugin.getLogger().info("==============================================");
        plugin.getLogger().info("EssentialsX兼容模式导入完成：工具包 " + kits + " 个 / 传送点 " + warps + " 个 / 玩家家 " + homes + " 个");
        plugin.getLogger().info("已导入的数据以Neko数据为准，同名内容不会被覆盖；修改EssentialsX配置后可使用 /nekoessentialx reload 重新导入新增数据");
        plugin.getLogger().info("==============================================");
    }

    // ====================== 工具包导入 ======================

    private int importKits() {
        File kitsFile = new File(essentialsFolder, "kits.yml");
        if (!kitsFile.isFile()) {
            return 0;
        }
        
        YamlConfiguration essKits;
        try {
            essKits = YamlConfiguration.loadConfiguration(kitsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("读取EssentialsX kits.yml失败：" + e.getMessage());
            return 0;
        }
        
        ConfigurationSection kitsSection = essKits.getConfigurationSection("kits");
        if (kitsSection == null) {
            return 0;
        }
        
        int imported = 0;
        for (String kitName : kitsSection.getKeys(false)) {
            String effectiveName = kitName.toLowerCase(Locale.ENGLISH);
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
            if (kitSection == null) {
                continue;
            }
            
            List<String> converted = new ArrayList<>();
            for (String item : kitSection.getStringList("items")) {
                String convertedItem = convertEssentialsItem(item);
                if (convertedItem != null) {
                    converted.add(convertedItem);
                } else if (item.startsWith("$") || item.startsWith("/") || item.startsWith("@") || item.startsWith("slot:")) {
                    plugin.getLogger().info("  - 工具包 '" + kitName + "' 跳过不兼容条目（金钱/命令/序列化物品）：" + item);
                } else {
                    plugin.getLogger().warning("  - 工具包 '" + kitName + "' 无法解析的物品（已跳过）：" + item);
                }
            }
            
            if (converted.isEmpty()) {
                plugin.getLogger().warning("  - 工具包 '" + kitName + "' 没有可导入的物品，已跳过");
                continue;
            }
            
            long cooldown = kitSection.getLong("delay", 0);
            // EssentialsX 的工具包默认权限节点为 essentials.kits.<名称>，导入后保留该权限以保证原有权限配置生效
            plugin.getKitManager().importKit(effectiveName, converted, cooldown, "essentials.kits." + effectiveName);
            imported++;
        }
        return imported;
    }

    /**
     * 将 EssentialsX 物品字符串（MATERIAL AMOUNT 附魔:等级...）转换为 Neko 格式（MATERIAL:AMOUNT:附魔1:等级1,附魔2:等级2）
     */
    private String convertEssentialsItem(String essItem) {
        if (essItem == null || essItem.trim().isEmpty()) {
            return null;
        }
        
        String line = essItem.trim();
        if (line.startsWith("$") || line.startsWith("/") || line.startsWith("@") || line.startsWith("slot:")) {
            return null;
        }
        
        try {
            String[] parts = line.split(" +");
            if (parts.length == 0) {
                return null;
            }
            
            String material = parts[0].toUpperCase(Locale.ENGLISH);
            if (Material.matchMaterial(material) == null) {
                return null;
            }
            
            StringBuilder result = new StringBuilder(material);
            int index = 1;
            if (parts.length > 1) {
                try {
                    int amount = Integer.parseInt(parts[1]);
                    result.append(":").append(Math.max(1, amount));
                    index = 2;
                } catch (NumberFormatException ignored) {
                    result.append(":1");
                }
            } else {
                result.append(":1");
            }
            
            List<String> enchantments = new ArrayList<>();
            for (int i = index; i < parts.length; i++) {
                String part = parts[i];
                String[] enchantInfo = part.split(":", 2);
                if (enchantInfo.length == 2 && Enchantment.getByName(enchantInfo[0].toUpperCase(Locale.ENGLISH)) != null) {
                    enchantments.add(part);
                }
            }
            if (!enchantments.isEmpty()) {
                result.append(":").append(String.join(",", enchantments));
            }
            
            return result.toString();
        } catch (Exception e) {
            plugin.getLogger().warning("解析EssentialsX物品失败：" + essItem);
            return null;
        }
    }

    // ====================== 传送点导入 ======================

    private int importWarps() {
        int imported = 0;
        
        // 现代版本：plugins/Essentials/warps/<名称>.yml，每个传送点一个文件
        File warpsFolder = new File(essentialsFolder, "warps");
        if (warpsFolder.isDirectory()) {
            File[] warpFiles = warpsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
            if (warpFiles != null) {
                for (File warpFile : warpFiles) {
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(warpFile);
                        String warpName = config.getString("name", null);
                        if (warpName == null || warpName.isEmpty()) {
                            warpName = warpFile.getName().substring(0, warpFile.getName().length() - 4);
                        }
                        if (importWarp(warpName, config.getString("world", null), config.getDouble("x", 0), config.getDouble("y", 0), config.getDouble("z", 0), (float) config.getDouble("yaw", 0), (float) config.getDouble("pitch", 0))) {
                            imported++;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("读取EssentialsX传送点文件失败：" + warpFile.getName() + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
        
        // 旧版本：plugins/Essentials/warps.yml 单文件格式
        File legacyWarpsFile = new File(essentialsFolder, "warps.yml");
        if (legacyWarpsFile.isFile()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyWarpsFile);
                ConfigurationSection warpsSection = config.getConfigurationSection("warps");
                if (warpsSection != null) {
                    for (String warpName : warpsSection.getKeys(false)) {
                        ConfigurationSection warpSection = warpsSection.getConfigurationSection(warpName);
                        if (warpSection != null && importWarp(warpName, warpSection.getString("world", null), warpSection.getDouble("x", 0), warpSection.getDouble("y", 0), warpSection.getDouble("z", 0), (float) warpSection.getDouble("yaw", 0), (float) warpSection.getDouble("pitch", 0))) {
                            imported++;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("读取EssentialsX旧版warps.yml失败：" + e.getMessage());
            }
        }
        
        return imported;
    }

    private boolean importWarp(String warpName, String worldName, double x, double y, double z, float yaw, float pitch) {
        if (warpName == null || warpName.isEmpty() || worldName == null || worldName.isEmpty()) {
            return false;
        }
        if (plugin.getWarpManager().warpExists(warpName)) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("  - 传送点 '" + warpName + "' 所在世界 '" + worldName + "' 未加载，已跳过");
            return false;
        }
        return plugin.getWarpManager().createWarp(warpName, new Location(world, x, y, z, yaw, pitch), "EssentialsX兼容导入");
    }

    // ====================== 玩家家导入 ======================

    private int importHomes() {
        File userdataFolder = new File(essentialsFolder, "userdata");
        if (!userdataFolder.isDirectory()) {
            return 0;
        }
        
        File[] userFiles = userdataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        if (userFiles == null) {
            return 0;
        }
        
        int imported = 0;
        for (File userFile : userFiles) {
            YamlConfiguration config;
            try {
                config = YamlConfiguration.loadConfiguration(userFile);
            } catch (Exception e) {
                plugin.getLogger().warning("读取EssentialsX玩家数据失败：" + userFile.getName() + " (" + e.getMessage() + ")");
                continue;
            }
            
            String username = config.getString("lastAccountName", null);
            if (username == null || username.isEmpty()) {
                username = config.getString("last-account-name", null);
            }
            if (username == null || username.isEmpty()) {
                plugin.getLogger().warning("  - 无法识别玩家文件 " + userFile.getName() + " 的玩家名（缺少lastAccountName），已跳过");
                continue;
            }
            
            ConfigurationSection homesSection = config.getConfigurationSection("homes");
            if (homesSection == null) {
                continue;
            }
            
            for (String homeName : homesSection.getKeys(false)) {
                ConfigurationSection homeSection = homesSection.getConfigurationSection(homeName);
                if (homeSection == null) {
                    continue;
                }
                String world = homeSection.getString("world", null);
                if (world == null || world.isEmpty()) {
                    continue;
                }
                if (plugin.getDatabaseManager().hasPlayerHome(username, homeName)) {
                    continue;
                }
                double x = homeSection.getDouble("x", 0);
                double y = homeSection.getDouble("y", 0);
                double z = homeSection.getDouble("z", 0);
                float yaw = (float) homeSection.getDouble("yaw", 0);
                float pitch = (float) homeSection.getDouble("pitch", 0);
                if (plugin.getDatabaseManager().savePlayerHome(username, homeName, world, x, y, z, yaw, pitch)) {
                    imported++;
                }
            }
        }
        return imported;
    }
}
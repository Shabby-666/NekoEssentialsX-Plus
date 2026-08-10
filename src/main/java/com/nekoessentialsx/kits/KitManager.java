package com.nekoessentialsx.kits;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KitManager {
    private final NekoEssentialX plugin;
    private final File kitsFile;
    private YamlConfiguration kitsConfig;
    private final Map<UUID, Map<String, Long>> kitCooldowns = new HashMap<>();

    public KitManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    /**
     * 加载工具包配置
     */
    public void loadKits() {
        // 如果配置文件不存在，创建默认配置
        if (!kitsFile.exists()) {
            createDefaultKits();
        }
        
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    /**
     * 创建默认工具包配置
     */
    private void createDefaultKits() {
        YamlConfiguration defaultConfig = new YamlConfiguration();
        
        // 创建默认工具包
        defaultConfig.set("default.items.stone_sword", "STONE_SWORD");
        defaultConfig.set("default.items.stone_pickaxe", "STONE_PICKAXE");
        defaultConfig.set("default.items.stone_axe", "STONE_AXE");
        defaultConfig.set("default.items.bread", "BREAD:10");
        defaultConfig.set("default.cooldown", 0);
        
        // 创建VIP工具包
        defaultConfig.set("vip.items.iron_sword", "IRON_SWORD:1:sharpness:1,unbreaking:2");
        defaultConfig.set("vip.items.iron_pickaxe", "IRON_PICKAXE:1:efficiency:2,unbreaking:3");
        defaultConfig.set("vip.items.golden_apple", "GOLDEN_APPLE:2");
        defaultConfig.set("vip.items.diamond", "DIAMOND:5");
        defaultConfig.set("vip.cooldown", 86400); // 24小时
        defaultConfig.set("vip.permission", "nekoessentialsx.kit.vip");
        
        try {
            defaultConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("创建默认工具包配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存工具包配置
     */
    public void saveKits() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存工具包配置失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有可用的工具包名称
     * @return 工具包名称列表
     */
    public List<String> getKitNames() {
        return new ArrayList<>(kitsConfig.getKeys(false));
    }

    /**
     * 检查玩家是否可以领取工具包
     * @param player 玩家
     * @param kitName 工具包名称
     * @return 可以领取返回null，否则返回原因
     */
    public String canClaimKit(Player player, String kitName) {
        if (!kitsConfig.contains(kitName)) {
            return "§c呜...找不到名叫 '" + kitName + "' 的工具包的说喵~";
        }
        
        // 检查权限
        String permission = kitsConfig.getString(kitName + ".permission");
        if (permission != null && !player.hasPermission(permission)) {
            return "§c呜...主人没有权限领这个工具包的说~喵~";
        }
        
        // 检查冷却时间
        long cooldown = kitsConfig.getLong(kitName + ".cooldown", 0);
        if (cooldown > 0) {
            long lastClaim = getLastClaimTime(player, kitName);
            long currentTime = System.currentTimeMillis();
            long remaining = cooldown * 1000 - (currentTime - lastClaim);
            
            if (remaining > 0) {
                return "§c呜...主人还要再等等 §e" + formatTime(remaining) + "§c 才能再领这个工具包的说~喵~";
            }
        }
        
        return null;
    }

    /**
     * 给玩家发放工具包
     * @param player 玩家
     * @param kitName 工具包名称
     * @return 是否发放成功
     */
    public boolean giveKit(Player player, String kitName) {
        String canClaim = canClaimKit(player, kitName);
        if (canClaim != null) {
            player.sendMessage(canClaim);
            return false;
        }
        
        // 发放物品
        if (!kitsConfig.contains(kitName + ".items")) {
            player.sendMessage("§c呜...这个工具包是空空的，什么都没有的说喵~");
            return false;
        }
        
        Map<String, Object> items = kitsConfig.getConfigurationSection(kitName + ".items").getValues(false);
        
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            String itemString = entry.getValue().toString();
            ItemStack item = parseItemString(itemString);
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
        
        // 更新冷却时间
        long cooldown = kitsConfig.getLong(kitName + ".cooldown", 0);
        if (cooldown > 0) {
            setLastClaimTime(player, kitName, System.currentTimeMillis());
        }
        
        player.sendMessage("§a呜呼~成功领到工具包 '§6" + kitName + "§a' 啦喵~");
        return true;
    }

    /**
     * 解析物品字符串为ItemStack
     * 格式：MATERIAL:AMOUNT:ENCHANTMENT1:LEVEL1,ENCHANTMENT2:LEVEL2
     */
    private ItemStack parseItemString(String itemString) {
        try {
            String[] parts = itemString.split(":");
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            
            ItemStack item = new ItemStack(material, amount);
            
            // 添加附魔
            if (parts.length > 2) {
                String enchantments = parts[2];
                for (String enchantPart : enchantments.split(",")) {
                    String[] enchantInfo = enchantPart.split(":");
                    if (enchantInfo.length >= 2) {
                        Enchantment enchantment = Enchantment.getByName(enchantInfo[0].toUpperCase());
                        if (enchantment != null) {
                            int level = Integer.parseInt(enchantInfo[1]);
                            item.addUnsafeEnchantment(enchantment, level);
                        }
                    }
                }
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("解析物品字符串失败：" + itemString);
            return null;
        }
    }

    /**
     * 获取玩家上次领取工具包的时间
     */
    private long getLastClaimTime(Player player, String kitName) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = kitCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        return playerCooldowns.getOrDefault(kitName, 0L);
    }

    /**
     * 设置玩家上次领取工具包的时间
     */
    private void setLastClaimTime(Player player, String kitName, long time) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = kitCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        playerCooldowns.put(kitName, time);
    }

    /**
     * 格式化时间
     */
    private String formatTime(long milliseconds) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        long hours = TimeUnit.MINUTES.toHours(minutes);
        long days = TimeUnit.HOURS.toDays(hours);
        
        if (days > 0) {
            return days + "天 " + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时 " + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟 " + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 导入外部工具包（兼容模式用，已存在的工具包不会被覆盖）
     * @param kitName 工具包名称
     * @param items 物品字符串列表（Neko格式：MATERIAL:AMOUNT:附魔:等级）
     * @param cooldown 冷却时间（秒）
     * @param permission 所需权限节点，可为null
     */
    public void importKit(String kitName, List<String> items, long cooldown, String permission) {
        if (kitsConfig.contains(kitName)) {
            return;
        }
        
        for (int i = 0; i < items.size(); i++) {
            kitsConfig.set(kitName + ".items.item" + i, items.get(i));
        }
        kitsConfig.set(kitName + ".cooldown", cooldown);
        if (permission != null && !permission.isEmpty()) {
            kitsConfig.set(kitName + ".permission", permission);
        }
        saveKits();
    }

    /**
     * 重新加载工具包配置
     */
    public void reload() {
        loadKits();
    }
}
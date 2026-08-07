package com.nekoessentialsx.newbiegift;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class NewbieGiftManager {
    private final NekoEssentialX plugin;
    private final DatabaseManager databaseManager;
    private File giftPackFile;
    private FileConfiguration giftPackConfig;
    private boolean enabled = false;
    private List<GiftItem> giftItems = new ArrayList<>();

    public NewbieGiftManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 启用新手礼包管理器
     */
    public void onEnable() {
        // 加载配置文件
        setupConfig();
        
        // 加载礼包物品
        loadGiftItems();
        
        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(new NewbieGiftListener(plugin, this), plugin);
        
        enabled = true;
        plugin.getLogger().info("新手礼包管理器已启用！喵~");
    }

    /**
     * 禁用新手礼包管理器
     */
    public void onDisable() {
        enabled = false;
        plugin.getLogger().info("新手礼包管理器已禁用！喵~");
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        setupConfig();
        loadGiftItems();
        plugin.getLogger().info("新手礼包配置已重载！喵~");
    }

    /**
     * 设置配置文件
     */
    private void setupConfig() {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        giftPackFile = new File(plugin.getDataFolder(), "newbiegiftpack.yml");
        
        try {
            // 从JAR文件中保存默认配置到数据文件夹
            if (!giftPackFile.exists()) {
                plugin.saveResource("newbiegiftpack.yml", false);
            }
            
            // 加载配置文件
            giftPackConfig = YamlConfiguration.loadConfiguration(giftPackFile);
            plugin.getLogger().info("成功加载newbiegiftpack.yml配置文件！喵~");
        } catch (Exception e) {
            plugin.getLogger().severe("加载newbiegiftpack.yml配置文件失败: " + e.getMessage());
            e.printStackTrace();
            // 创建一个空的配置作为回退
            giftPackConfig = new YamlConfiguration();
        }
    }

    /**
     * 加载礼包物品
     */
    private void loadGiftItems() {
        giftItems.clear();
        
        if (giftPackConfig.contains("items")) {
            for (String itemId : giftPackConfig.getConfigurationSection("items").getKeys(false)) {
                try {
                    // 获取物品配置
                    String path = "items." + itemId;
                    String name = giftPackConfig.getString(path + ".name", itemId);
                    String materialName = giftPackConfig.getString(path + ".material");
                    int amount = giftPackConfig.getInt(path + ".amount", 1);
                    double probability = giftPackConfig.getDouble(path + ".probability", 1.0);
                    List<String> lore = giftPackConfig.getStringList(path + ".lore");
                    Map<Enchantment, Integer> enchants = new HashMap<>();
                    
                    // 加载附魔
                    if (giftPackConfig.contains(path + ".enchants")) {
                        for (String enchantName : giftPackConfig.getConfigurationSection(path + ".enchants").getKeys(false)) {
                            try {
                                Enchantment enchant = Enchantment.getByName(enchantName.toUpperCase());
                                if (enchant != null) {
                                    int level = giftPackConfig.getInt(path + ".enchants." + enchantName, 1);
                                    enchants.put(enchant, level);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("加载附魔" + enchantName + "失败: " + e.getMessage());
                            }
                        }
                    }
                    
                    // 创建GiftItem对象并添加到列表
                    if (materialName != null) {
                        Material material = Material.matchMaterial(materialName);
                        if (material != null) {
                            GiftItem giftItem = new GiftItem(itemId, name, material, amount, probability, enchants, lore);
                            giftItems.add(giftItem);
                        } else {
                            plugin.getLogger().warning("未知的物品材质: " + materialName);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("加载物品" + itemId + "失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        plugin.getLogger().info("已加载 " + giftItems.size() + " 个新手礼包物品！喵~");
    }

    /**
     * 检查玩家是否可以领取新手礼包
     * @param playerName 玩家名称
     * @return 是否可以领取
     */
    public boolean canClaimGift(String playerName) {
        return !databaseManager.hasClaimedNewbieGift(playerName);
    }

    /**
     * 处理玩家领取新手礼包
     * @param player 玩家对象
     */
    public void handleGiftClaim(Player player) {
        String playerName = player.getName();
        
        // 检查是否可以领取
        if (!canClaimGift(playerName)) {
            player.sendMessage("§c呜...你已经领过新手礼包了的说~喵~");
            return;
        }
        
        // 发送消息
        player.sendMessage("§a呜...正在给主人开箱开箱，请稍等的说~喵~");
        
        // 使用Bukkit调度器实现3秒延迟
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // 随机选择15个不同的物品
                List<GiftItem> selectedItems = selectRandomItems(15);
                
                // 记录玩家已领取礼包
                if (!databaseManager.markNewbieGiftClaimed(playerName)) {
                    player.sendMessage("§c呜...领取失败了的说，想一下再试啦喵~");
                    return;
                }
                
                // 给玩家发送确认消息
                StringBuilder sb = new StringBuilder("§a呜呼~主人成功领到新手大礼包啦：");
                for (GiftItem item : selectedItems) {
                    sb.append("§b").append(item.getName()).append("§a, ");
                }
                // 移除最后一个逗号和空格
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 2);
                }
                player.sendMessage(sb.toString());
                
                // 给玩家添加物品
                for (GiftItem item : selectedItems) {
                    ItemStack itemStack = item.toItemStack();
                    player.getInventory().addItem(itemStack);
                }
                
                // 发送每日签到提示
                String currencyName = plugin.getEconomyManager().getCurrencyName();
                player.sendMessage("§a每天签到都能领1" + currencyName + "哦！记得天天来签到的说喵~");
                
                plugin.getLogger().info("玩家 " + playerName + " 成功领取了新手礼包！喵~");
            } catch (Exception e) {
                player.sendMessage("§c呜...领取新手礼包失败了的说，快去找管理员帮忙喵~");
                plugin.getLogger().severe("玩家 " + playerName + " 领取新手礼包失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, 20L * 3); // 20 ticks = 1秒，所以60 ticks = 3秒
    }

    /**
     * 随机选择指定数量的物品
     * @param count 物品数量
     * @return 选中的物品列表
     */
    private List<GiftItem> selectRandomItems(int count) {
        List<GiftItem> result = new ArrayList<>();
        List<GiftItem> availableItems = new ArrayList<>(giftItems);
        
        // 如果可用物品数量小于请求数量，返回所有可用物品
        if (availableItems.size() <= count) {
            return availableItems;
        }
        
        // 随机选择指定数量的不同物品
        Random random = new Random();
        while (result.size() < count && !availableItems.isEmpty()) {
            int index = random.nextInt(availableItems.size());
            GiftItem item = availableItems.get(index);
            
            // 根据概率决定是否选中该物品
            if (random.nextDouble() <= item.getProbability()) {
                result.add(item);
            }
            
            // 移除已选中的物品，确保每个物品只被选中一次
            availableItems.remove(index);
        }
        
        // 如果选中的物品数量不足，补充剩余数量
        while (result.size() < count && !giftItems.isEmpty()) {
            int index = random.nextInt(giftItems.size());
            GiftItem item = giftItems.get(index);
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        
        return result;
    }

    /**
     * 获取随机物品（用于GUI动画）
     * @return 随机物品
     */
    public GiftItem getRandomGiftItem() {
        if (giftItems.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        return giftItems.get(random.nextInt(giftItems.size()));
    }

    /**
     * 获取所有礼包物品
     * @return 礼包物品列表
     */
    public List<GiftItem> getGiftItems() {
        return giftItems;
    }

    /**
     * 获取管理器是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 礼包物品类
     */
    public static class GiftItem {
        private final String id;
        private final String name;
        private final Material material;
        private final int amount;
        private final double probability;
        private final Map<Enchantment, Integer> enchants;
        private final List<String> lore;

        public GiftItem(String id, String name, Material material, int amount, double probability, 
                       Map<Enchantment, Integer> enchants, List<String> lore) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.amount = amount;
            this.probability = probability;
            this.enchants = enchants;
            this.lore = lore;
        }

        /**
         * 转换为ItemStack对象
         * @return ItemStack对象
         */
        public ItemStack toItemStack() {
            ItemStack itemStack = new ItemStack(material, amount);
            ItemMeta meta = itemStack.getItemMeta();
            
            if (meta != null) {
                // 设置显示名称
                meta.setDisplayName(name);
                
                // 设置描述
                if (lore != null && !lore.isEmpty()) {
                    meta.setLore(lore);
                }
                
                // 设置附魔
                if (enchants != null && !enchants.isEmpty()) {
                    for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
                
                itemStack.setItemMeta(meta);
            }
            
            return itemStack;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public double getProbability() { return probability; }
        public Map<Enchantment, Integer> getEnchants() { return enchants; }
        public List<String> getLore() { return lore; }
    }
}
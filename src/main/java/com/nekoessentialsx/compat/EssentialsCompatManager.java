package com.nekoessentialsx.compat;

import com.nekoessentialsx.NekoEssentialX;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EssentialsCompatManager {
    private final NekoEssentialX plugin;
    private final File essentialsFolder;
    private final Map<String, String> itemAliases = new HashMap<>();
    private final Map<String, String> enchantAliases = new HashMap<>();

    public EssentialsCompatManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials");
        loadAliases();
        loadEnchantAliases();
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
        int eco = importEconomy();
        plugin.getLogger().info("==============================================");
        plugin.getLogger().info("EssentialsX兼容模式导入完成：工具包 " + kits + " 个 / 传送点 " + warps + " 个 / 玩家家 " + homes + " 个 / 经济 " + eco + " 人");
        plugin.getLogger().info("已导入的数据以Neko数据为准，同名内容不会被覆盖；修改EssentialsX配置后可使用 /nekoessentialx reload 重新导入新增数据");
        plugin.getLogger().info("==============================================");
    }

    // ====================== 别名加载 ======================

    /**
     * 从 EssentialsX 的 items.json 加载物品别名表
     * 格式：主名 {"material": "MATERIAL"}，别名 "alias": "主名"
     */
    private void loadAliases() {
        // 内置基础别名（兜底）
        buildDefaultAliases();
        
        // 尝试读取 EssentialsX 的 items.json
        File itemsFile = new File(essentialsFolder, "items.json");
        if (!itemsFile.isFile()) {
            plugin.getLogger().info("[兼容模式] 未找到 EssentialsX items.json，使用内置别名表");
            return;
        }
        
        try (Reader reader = new FileReader(itemsFile)) {
            // 跳过注释行（items.json 以 #version: ... 开头）
            StringBuilder sb = new StringBuilder();
            String line;
            java.io.BufferedReader br = new java.io.BufferedReader(reader);
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    sb.append(line);
                }
            }
            
            JsonObject map = JsonParser.parseString(sb.toString()).getAsJsonObject();
            Map<String, String> primaryNames = new HashMap<>();
            
            // 第一遍：收集主名（带 material 字段的）
            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ENGLISH);
                JsonElement element = entry.getValue();
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("material")) {
                        primaryNames.put(key, obj.get("material").getAsString());
                    }
                }
            }
            
            // 第二遍：别名指向主名，主名直接指向 material
            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ENGLISH);
                JsonElement element = entry.getValue();
                if (element.isJsonPrimitive()) {
                    String target = element.getAsString().toLowerCase(Locale.ENGLISH);
                    String material = primaryNames.get(target);
                    if (material != null) {
                        itemAliases.put(key, material);
                    }
                } else if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("material")) {
                        itemAliases.put(key, obj.get("material").getAsString());
                    }
                }
            }
            
            plugin.getLogger().info("[兼容模式] 从 EssentialsX items.json 加载了 " + itemAliases.size() + " 条物品别名喵~");
        } catch (Exception e) {
            plugin.getLogger().warning("[兼容模式] 读取 EssentialsX items.json 失败：" + e.getMessage() + "喵~");
        }
    }

    /**
     * 内置基础别名表（当 items.json 不可用时兜底）
     */
    private void buildDefaultAliases() {
        String[][] aliases = {
            {"stonesword", "STONE_SWORD"}, {"ssword", "STONE_SWORD"}, {"cobblestonesword", "STONE_SWORD"},
            {"stonepickaxe", "STONE_PICKAXE"}, {"cstonepickaxe", "STONE_PICKAXE"},
            {"stoneaxe", "STONE_AXE"}, {"cstoneaxe", "STONE_AXE"},
            {"stoneshovel", "STONE_SHOVEL"}, {"cstoneshovel", "STONE_SHOVEL"},
            {"ironsword", "IRON_SWORD"}, {"isword", "IRON_SWORD"},
            {"ironpickaxe", "IRON_PICKAXE"}, {"ipickaxe", "IRON_PICKAXE"},
            {"ironaxe", "IRON_AXE"}, {"iaxe", "IRON_AXE"},
            {"ironshovel", "IRON_SHOVEL"}, {"ishovel", "IRON_SHOVEL"},
            {"diamondsword", "DIAMOND_SWORD"}, {"dsword", "DIAMOND_SWORD"},
            {"diamondpickaxe", "DIAMOND_PICKAXE"}, {"dpickaxe", "DIAMOND_PICKAXE"},
            {"diamondaxe", "DIAMOND_AXE"}, {"daxe", "DIAMOND_AXE"},
            {"diamondshovel", "DIAMOND_SHOVEL"}, {"dshovel", "DIAMOND_SHOVEL"},
            {"goldsword", "GOLDEN_SWORD"}, {"gsword", "GOLDEN_SWORD"},
            {"goldpickaxe", "GOLDEN_PICKAXE"}, {"gpickaxe", "GOLDEN_PICKAXE"},
            {"goldaxe", "GOLDEN_AXE"}, {"gaxe", "GOLDEN_AXE"},
            {"goldshovel", "GOLDEN_SHOVEL"}, {"gshovel", "GOLDEN_SHOVEL"},
            {"woodsword", "WOODEN_SWORD"}, {"wsword", "WOODEN_SWORD"},
            {"woodpickaxe", "WOODEN_PICKAXE"}, {"wpickaxe", "WOODEN_PICKAXE"},
            {"woodaxe", "WOODEN_AXE"}, {"waxe", "WOODEN_AXE"},
            {"woodshovel", "WOODEN_SHOVEL"}, {"wshovel", "WOODEN_SHOVEL"},
            {"leatherset", "LEATHER_CHESTPLATE"},
            {"woodenshovel", "WOODEN_SHOVEL"},
            {"sticks", "STICK"}, {"cobble", "COBBLESTONE"},
            {"stone", "STONE"}, {"dirt", "DIRT"},
            {"glass", "GLASS"}, {"ironbars", "IRON_BARS"},
            {"workbench", "CRAFTING_TABLE"}, {"bench", "CRAFTING_TABLE"},
            {"furnace", "FURNACE"}, {"enchant", "ENCHANTING_TABLE"},
            {"anvil", "ANVIL"}, {"chest", "CHEST"},
            {"trappedchest", "TRAPPED_CHEST"},
            {"enderchest", "ENDER_CHEST"},
            {"playerhead", "PLAYER_HEAD"}, {"skull", "PLAYER_HEAD"},
            {"writtenbook", "WRITTEN_BOOK"},
            {"goldenapple", "GOLDEN_APPLE"}, {"gapple", "GOLDEN_APPLE"},
            {"enchantedgoldenapple", "ENCHANTED_GOLDEN_APPLE"}, {"egapple", "ENCHANTED_GOLDEN_APPLE"},
            {"enderpearl", "ENDER_PEARL"},
            {"eyesofender", "EYE_OF_ENDER"},
            {"ironingot", "IRON_INGOT"}, {"goldingot", "GOLD_INGOT"},
            {"diamond", "DIAMOND"}, {"emerald", "EMERALD"},
            {"redstone", "REDSTONE"}, {"glowstone", "GLOWSTONE_DUST"},
            {"string", "STRING"}, {"feather", "FEATHER"},
            {"flint", "FLINT"}, {"gunpowder", "GUNPOWDER"},
            {"leather", "LEATHER"}, {"clay", "CLAY_BALL"},
            {"brick", "BRICK"}, {"paper", "PAPER"},
            {"book", "BOOK"}, {"slimeball", "SLIME_BALL"},
            {"mushroom", "RED_MUSHROOM"}, {"flower", "DANDELION"},
            {"sapling", "OAK_SAPLING"}, {"log", "OAK_LOG"},
            {"planks", "OAK_PLANKS"}, {"fence", "OAK_FENCE"},
            {"gate", "OAK_FENCE_GATE"}, {"stairs", "OAK_STAIRS"},
            {"torch", "TORCH"}, {"ladder", "LADDER"},
            {"rail", "RAIL"}, {"poweredrail", "POWERED_RAIL"},
            {"detrail", "DETECTOR_RAIL"}, {"activatorrail", "ACTIVATOR_RAIL"},
            {"tnt", "TNT"}, {"obsidian", "OBSIDIAN"},
            {"endstone", "END_STONE"}, {"netherrack", "NETHERRACK"},
            {"soul sand", "SOUL_SAND"}, {"glowstone", "GLOWSTONE_DUST"},
            {"netherwart", "NETHER_WART"},
            {"brewingstand", "BREWING_STAND"}, {"cauldron", "CAULDRON"},
            {"sign", "OAK_SIGN"}, {"bed", "RED_BED"},
            {"bookshelf", "BOOKSHELF"},
            {"snowball", "SNOWBALL"}, {"egg", "EGG"},
            {"bow", "BOW"}, {"arrow", "ARROW"},
            {"fishingrod", "FISHING_ROD"}, {"flintsteel", "FLINT_AND_STEEL"},
            {"clock", "CLOCK"}, {"compass", "COMPASS"},
            {"map", "MAP"}, {"shears", "SHEARS"},
            {"carrotstick", "CARROT_ON_A_STICK"},
            {"saddle", "SADDLE"}, {"horsearmor", "IRON_HORSE_ARMOR"},
            {"prismarine", "PRISMARINE_SHARD"},
            {"prismarinecrystals", "PRISMARINE_CRYSTALS"},
            {"nautilusshell", "NAUTILUS_SHELL"},
            {"trident", "TRIDENT"}, {"crossbow", "CROSSBOW"},
            {"shield", "SHIELD"}, {"totem", "TOTEM_OF_UNDYING"},
            {"shulkerbox", "SHULKER_BOX"},
            {"elytra", "ELYTRA"},
            {"beacon", "BEACON"}, {"conduit", "CONDUIT"},
            {"shard", "PRISMARINE_SHARD"},
            {"crystals", "PRISMARINE_CRYSTALS"},
        };
        for (String[] a : aliases) {
            itemAliases.putIfAbsent(a[0], a[1]);
        }
    }

    /**
     * 加载 EssentialsX 附魔别名
     */
    private void loadEnchantAliases() {
        String[][] enchants = {
            {"digspeed", "EFFICIENCY"}, {"efficiency", "EFFICIENCY"},
            {"durability", "UNBREAKING"}, {"unbreaking", "UNBREAKING"},
            {"sharpness", "SHARPNESS"}, {"smite", "SMITE"},
            {"baneofarthropods", "BANE_OF_ARTHROPODS"}, {"bane", "BANE_OF_ARTHROPODS"},
            {"fireaspect", "FIRE_ASPECT"}, {"fire", "FIRE_ASPECT"},
            {"looting", "LOOTING"}, {"silk", "SILK_TOUCH"}, {"silktouch", "SILK_TOUCH"},
            {"fortune", "FORTUNE"}, {"power", "POWER"},
            {"flame", "FLAME"}, {"infinity", "INFINITY"},
            {"punch", "PUNCH"}, {"knockback", "KNOCKBACK"},
            {"firearrow", "FLAME"}, {"slowfalling", "SLOW_FALLING"},
            {"thorns", "THORNS"}, {"protection", "PROTECTION"},
            {"blastprotection", "BLAST_PROTECTION"},
            {"fireprotection", "FIRE_PROTECTION"},
            {"projectileprotection", "PROJECTILE_PROTECTION"},
            {"respiration", "RESPIRATION"}, {"aqua", "AQUA_AFFINITY"},
            {"depth", "DEPTH_STRIDER"}, {"depthstrider", "DEPTH_STRIDER"},
            {"frostwalker", "FROST_WALKER"}, {"frost", "FROST_WALKER"},
            {"soulbound", "SOULBOUND"},
            {"lure", "LURE"}, {"luckofthesea", "LUCK_OF_THE_SEA"},
            {"mending", "MENDING"},
            {"sweeping", "SWEEPING_EDGE"}, {"sweepingedge", "SWEEPING_EDGE"},
            {"impaling", "IMPALING"},
            {"loyalty", "LOYALTY"}, {"channeling", "CHANNELING"},
            {"riptide", "RIPTIDE"},
            {"quickcharge", "QUICK_CHARGE"},
            {"piercing", "PIERCING"},
            {"multishot", "MULTISHOT"},
            {"vanishing", "VANISHING_CURSE"}, {"vanishingcurse", "VANISHING_CURSE"},
            {"cursofvanishing", "VANISHING_CURSE"},
            {"binding", "BINDING_CURSE"}, {"bindingcurse", "BINDING_CURSE"},
            {"cursofbinding", "BINDING_CURSE"},
            {"flying", "FIREWORK"},
            {"frost", "FROST_WALKER"},
            {"iceaspects", "ICE_ASPECT"},
        };
        for (String[] e : enchants) {
            enchantAliases.put(e[0], e[1]);
        }
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
            plugin.getKitManager().importKit(effectiveName, converted, cooldown, "essentials.kits." + effectiveName);
            imported++;
        }
        return imported;
    }

    /**
     * 将 EssentialsX 物品字符串转换为 Neko pipe-delimited 格式
     * EssentialsX: MATERIAL AMOUNT [meta...]
     * Neko:        MATERIAL:AMOUNT:ENCH:LVL|name:xxx|lore:xxx|player:xxx|unbreakable|firework:color:red,fade:green,shape: creeper
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
            String[] tokens = line.split(" +");
            if (tokens.length == 0) {
                return null;
            }
            
            // 第一步：解析物品名称（支持 EssentialsX 别名，尝试 1~2 个 token）
            // EssentialsX 格式：materialName:damage amount [meta...]
            int materialEnd = 0;
            String materialName = null;
            for (int tryLen = 1; tryLen <= Math.min(2, tokens.length); tryLen++) {
                StringBuilder sb = new StringBuilder(tokens[0]);
                for (int j = 1; j < tryLen; j++) {
                    sb.append(" ").append(tokens[j]);
                }
                String rawToken = sb.toString();
                
                // 尝试直接解析
                String resolved = resolveItemName(rawToken);
                if (resolved != null) {
                    materialName = resolved;
                    materialEnd = tryLen;
                    break;
                }
                
                // 尝试去掉 :damage 部分后解析（daxe:780 → daxe）
                int colonIdx = rawToken.indexOf(':');
                if (colonIdx > 0) {
                    resolved = resolveItemName(rawToken.substring(0, colonIdx));
                    if (resolved != null) {
                        materialName = resolved;
                        materialEnd = tryLen;
                        break;
                    }
                }
            }
            if (materialName == null) {
                return null;
            }
            
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                return null;
            }
            
            // 第二步：解析数量
            int amount = 1;
            int metaStart = materialEnd;
            if (metaStart < tokens.length) {
                try {
                    amount = Math.max(1, Integer.parseInt(tokens[metaStart]));
                    metaStart++;
                } catch (NumberFormatException ignored) {
                }
            }
            
            // 第三步：收集元数据
            List<String> enchParts = new ArrayList<>();
            List<String> metaParts = new ArrayList<>();
            boolean isFirework = material.name().contains("FIREWORK");
            boolean isPotion = material.name().contains("POTION") || material.name().equals("SPLASH_POTION")
                    || material.name().equals("LINGERING_POTION") || material.name().equals("TIPPED_ARROW");
            
            // 烟花效果收集
            List<String[]> currentEffect = new ArrayList<>();
            String fireworkPower = null;
            
            // 药水效果收集
            List<String[]> potionProps = new ArrayList<>();
            
            for (int i = metaStart; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.isEmpty()) continue;
                
                // 无冒号的 token：unbreakable / {NBT}
                if (!token.contains(":")) {
                    if (token.equalsIgnoreCase("unbreakable")) {
                        metaParts.add("unbreakable");
                    }
                    continue;
                }
                
                int colonIdx = token.indexOf(':');
                String key = token.substring(0, colonIdx).toLowerCase(Locale.ENGLISH);
                String value = token.substring(colonIdx + 1);
                
                // 烟花物品：收集 color/fade/shape/type/effect/power 到分组中
                if (isFirework) {
                    if (key.equals("power") || key.equals("p")) {
                        fireworkPower = value;
                        continue;
                    }
                    if (key.equals("color") || key.equals("colour") || key.equals("c")) {
                        // color 开始新效果：先提交上一组
                        if (!currentEffect.isEmpty()) {
                            commitFireworkEffect(metaParts, currentEffect);
                            currentEffect = new ArrayList<>();
                        }
                        currentEffect.add(new String[]{"color", value});
                        continue;
                    }
                    if (key.equals("fade") || key.equals("f")) {
                        currentEffect.add(new String[]{"fade", value});
                        continue;
                    }
                    if (key.equals("shape") || key.equals("type") || key.equals("s") || key.equals("t")) {
                        currentEffect.add(new String[]{"shape", value});
                        continue;
                    }
                    if (key.equals("effect") || key.equals("e")) {
                        currentEffect.add(new String[]{"effect", value});
                        continue;
                    }
                }
                
                // 药水物品：收集 effect/power/amplifier/duration/splash
                if (isPotion) {
                    if (key.equals("effect") || key.equals("e")) {
                        potionProps.add(new String[]{"effect", value});
                        continue;
                    }
                    if (key.equals("power") || key.equals("p")) {
                        potionProps.add(new String[]{"power", value});
                        continue;
                    }
                    if (key.equals("amplifier") || key.equals("a")) {
                        potionProps.add(new String[]{"amplifier", value});
                        continue;
                    }
                    if (key.equals("duration") || key.equals("d")) {
                        potionProps.add(new String[]{"duration", value});
                        continue;
                    }
                    if (key.equals("splash") || key.equals("s")) {
                        potionProps.add(new String[]{"splash", value});
                        continue;
                    }
                }
                
                // 通用元数据标签
                if (key.equals("name")) {
                    metaParts.add("name:" + value);
                    continue;
                }
                if (key.equals("lore") || key.equals("desc")) {
                    metaParts.add("lore:" + value);
                    continue;
                }
                if (key.equals("player") || key.equals("owner")) {
                    metaParts.add("player:" + value);
                    continue;
                }
                if (key.equals("author")) {
                    metaParts.add("author:" + value);
                    continue;
                }
                if (key.equals("title")) {
                    metaParts.add("title:" + value);
                    continue;
                }
                if (key.equals("book")) {
                    metaParts.add("book:" + value);
                    continue;
                }
                if (key.equals("cmd") || key.equals("custom-model-data")) {
                    metaParts.add("cmd:" + value);
                    continue;
                }
                if (key.equals("itemflags")) {
                    metaParts.add("itemflags:" + value);
                    continue;
                }
                if (key.equals("trim")) {
                    metaParts.add("trim:" + value);
                    continue;
                }
                if (key.equals("color") || key.equals("colour")) {
                    metaParts.add("color:" + value);
                    continue;
                }
                if (key.startsWith("page") && key.length() > 4 && Character.isDigit(key.charAt(4))) {
                    metaParts.add(token);
                    continue;
                }
                
                // 非烟花物品的短格式别名
                if (key.equals("f")) {
                    metaParts.add("fade:" + value);
                    continue;
                }
                if (key.equals("s") || key.equals("t")) {
                    metaParts.add("shape:" + value);
                    continue;
                }
                if (key.equals("e")) {
                    metaParts.add("effect:" + value);
                    continue;
                }
                if (key.equals("p")) {
                    metaParts.add("power:" + value);
                    continue;
                }
                if (key.equals("a")) {
                    metaParts.add("amplifier:" + value);
                    continue;
                }
                if (key.equals("d")) {
                    metaParts.add("duration:" + value);
                    continue;
                }
                
                // {JSON NBT} 或 [Components] — 跳过
                if (token.startsWith("{") || token.startsWith("[")) {
                    continue;
                }
                
                // 回退：尝试作为附魔
                String enchName = resolveEnchantName(key);
                if (enchName != null) {
                    String lvl = value.isEmpty() ? "" : value;
                    enchParts.add(enchName + (lvl.isEmpty() ? "" : ":" + lvl));
                }
            }
            
            // 提交最后一组烟花效果
            if (isFirework && !currentEffect.isEmpty()) {
                commitFireworkEffect(metaParts, currentEffect);
            }
            
            // 提交药水效果
            if (isPotion && !potionProps.isEmpty()) {
                StringBuilder pSb = new StringBuilder("potion:");
                for (int j = 0; j < potionProps.size(); j++) {
                    if (j > 0) pSb.append(",");
                    pSb.append(potionProps.get(j)[0]).append(":").append(potionProps.get(j)[1]);
                }
                metaParts.add(pSb.toString());
            }
            
            // 第四步：组装输出
            StringBuilder result = new StringBuilder(material.name());
            result.append(":").append(amount);
            if (!enchParts.isEmpty()) {
                result.append(":").append(String.join(",", enchParts));
            }
            for (String meta : metaParts) {
                result.append("|").append(meta);
            }
            if (fireworkPower != null) {
                result.append("|power:").append(fireworkPower);
            }
            
            return result.toString();
        } catch (Exception e) {
            plugin.getLogger().warning("解析EssentialsX物品失败：" + essItem);
            return null;
        }
    }
    
    /**
     * 将一组烟花效果提交为 firework: 格式
     * 格式：color:red,fade:green,shape: creeper,effect:twinkle; color:blue,shape:star
     */
    private void commitFireworkEffect(List<String> metaParts, List<String[]> effect) {
        if (effect.isEmpty()) return;
        StringBuilder sb = new StringBuilder("firework:");
        for (int j = 0; j < effect.size(); j++) {
            if (j > 0) sb.append(",");
            sb.append(effect.get(j)[0]).append(":").append(effect.get(j)[1]);
        }
        metaParts.add(sb.toString());
    }

    /**
     * 解析 EssentialsX 物品名称（别名 → Bukkit Material 名称）
     */
    private String resolveItemName(String name) {
        String lower = name.toLowerCase(Locale.ENGLISH);
        String material = itemAliases.get(lower);
        if (material != null) {
            return material;
        }
        // 尝试直接匹配 Bukkit Material（全大写）
        String upper = lower.toUpperCase(Locale.ENGLISH);
        if (Material.matchMaterial(upper) != null) {
            return upper;
        }
        return null;
    }

    /**
     * 解析 EssentialsX 附魔别名
     */
    private String resolveEnchantName(String name) {
        String lower = name.toLowerCase(Locale.ENGLISH);
        String resolved = enchantAliases.get(lower);
        if (resolved != null) {
            return resolved;
        }
        // 尝试直接匹配 Bukkit Enchantment 名称
        String upper = lower.toUpperCase(Locale.ENGLISH);
        if (Enchantment.getByName(upper) != null) {
            return upper;
        }
        return null;
    }

    // ====================== 经济导入 ======================

    private int importEconomy() {
        File userdataFolder = new File(essentialsFolder, "userdata");
        if (!userdataFolder.isDirectory()) {
            plugin.getLogger().info("[兼容模式] 未找到 userdata 文件夹");
            return 0;
        }

        File[] userFiles = userdataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".yml"));
        if (userFiles == null || userFiles.length == 0) {
            plugin.getLogger().info("[兼容模式] userdata 文件夹为空");
            return 0;
        }
        plugin.getLogger().info("[兼容模式] 发现 " + userFiles.length + " 个玩家数据文件");

        int imported = 0;
        for (File userFile : userFiles) {
            YamlConfiguration config;
            try {
                config = YamlConfiguration.loadConfiguration(userFile);
            } catch (Exception e) {
                plugin.getLogger().warning("[兼容模式] 读取失败：" + userFile.getName() + " - " + e.getMessage());
                continue;
            }

            String username = config.getString("lastAccountName", null);
            if (username == null || username.isEmpty()) {
                username = config.getString("last-account-name", null);
            }
            if (username == null || username.isEmpty()) {
                plugin.getLogger().info("[兼容模式] 跳过 " + userFile.getName() + ": 无玩家名");
                continue;
            }

            plugin.getLogger().info("[兼容模式] 玩家: " + username + " keys含money=" + config.contains("money") + " money原始=" + config.getString("money"));

            if (!config.contains("money")) {
                continue;
            }

            double money = 0.0;
            try {
                money = Double.parseDouble(config.getString("money", "0"));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[兼容模式] 玩家 " + username + " money格式错误: " + config.getString("money"));
                continue;
            }
            plugin.getLogger().info("[兼容模式] 玩家: " + username + " 余额=" + money);

            if (money <= 0.0) {
                continue;
            }

            double existing = plugin.getDatabaseManager().getPlayerBalance(username);
            plugin.getLogger().info("[兼容模式] 玩家: " + username + " 已有余额=" + existing);

            if (existing > 0.0) {
                continue;
            }

            if (plugin.getDatabaseManager().setPlayerBalance(username, money)) {
                imported++;
                plugin.getLogger().info("  - 导入经济：" + username + " = " + money);
            }
        }
        return imported;
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
                        // EssentialsX 的 world 键存储的是世界UUID，world-name 才是世界名称
                        String worldName = config.getString("world-name", null);
                        if (worldName == null || worldName.isEmpty()) {
                            worldName = resolveWorldName(config.getString("world", null));
                        }
                        double x = config.getDouble("x", 0);
                        double y = config.getDouble("y", 0);
                        double z = config.getDouble("z", 0);
                        float yaw = (float) config.getDouble("yaw", 0);
                        float pitch = (float) config.getDouble("pitch", 0);
                        if (importWarp(warpName, worldName, x, y, z, yaw, pitch)) {
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
                        if (warpSection != null) {
                            String worldName = warpSection.getString("world", null);
                            if (importWarp(warpName, worldName, warpSection.getDouble("x", 0), warpSection.getDouble("y", 0), warpSection.getDouble("z", 0), (float) warpSection.getDouble("yaw", 0), (float) warpSection.getDouble("pitch", 0))) {
                                imported++;
                            }
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
        World world = resolveWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("  - 传送点 '" + warpName + "' 所在世界 '" + worldName + "' 未加载，已跳过");
            return false;
        }
        return plugin.getWarpManager().createWarp(warpName, new Location(world, x, y, z, yaw, pitch), "EssentialsX兼容导入");
    }

    /**
     * 解析世界名称（支持 UUID 格式 → 世界名称）
     */
    private World resolveWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        // 先尝试直接名称
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        // 尝试 UUID
        try {
            UUID uuid = UUID.fromString(worldName);
            return Bukkit.getWorld(uuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 将 UUID 字符串解析为世界名称（非UUID则直接返回原值）
     */
    private String resolveWorldName(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        // 如果是 UUID，尝试解析为世界名称
        try {
            UUID uuid = UUID.fromString(value);
            World world = Bukkit.getWorld(uuid);
            return world != null ? world.getName() : null;
        } catch (IllegalArgumentException ignored) {
            // 不是 UUID，当作世界名称直接返回
            return value;
        }
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
                // EssentialsX 的 world 键存储的是世界UUID，world-name 才是世界名称
                String worldName = homeSection.getString("world-name", null);
                if (worldName == null || worldName.isEmpty()) {
                    worldName = resolveWorldName(homeSection.getString("world", null));
                }
                if (worldName == null || worldName.isEmpty()) {
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
                if (plugin.getDatabaseManager().savePlayerHome(username, homeName, worldName, x, y, z, yaw, pitch)) {
                    imported++;
                }
            }
        }
        return imported;
    }
}
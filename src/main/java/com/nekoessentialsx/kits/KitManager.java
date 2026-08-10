package com.nekoessentialsx.kits;

import com.nekoessentialsx.NekoEssentialX;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.BlockState;
import org.bukkit.block.Banner;
import org.bukkit.DyeColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    public void loadKits() {
        if (!kitsFile.exists()) {
            createDefaultKits();
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
    }

    private void createDefaultKits() {
        YamlConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.set("default.items.stone_sword", "STONE_SWORD");
        defaultConfig.set("default.items.stone_pickaxe", "STONE_PICKAXE");
        defaultConfig.set("default.items.stone_axe", "STONE_AXE");
        defaultConfig.set("default.items.bread", "BREAD:10");
        defaultConfig.set("default.cooldown", 0);
        defaultConfig.set("vip.items.iron_sword", "IRON_SWORD:1:sharpness:1|unbreakable");
        defaultConfig.set("vip.items.iron_pickaxe", "IRON_PICKAXE:1:efficiency:2|unbreakable");
        defaultConfig.set("vip.items.golden_apple", "GOLDEN_APPLE:2");
        defaultConfig.set("vip.items.diamond", "DIAMOND:5");
        defaultConfig.set("vip.cooldown", 86400);
        defaultConfig.set("vip.permission", "nekoessentialsx.kit.vip");
        try {
            defaultConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("创建默认工具包配置失败：" + e.getMessage());
        }
    }

    public void saveKits() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存工具包配置失败：" + e.getMessage());
        }
    }

    public List<String> getKitNames() {
        return new ArrayList<>(kitsConfig.getKeys(false));
    }

    public String canClaimKit(Player player, String kitName) {
        if (!kitsConfig.contains(kitName)) {
            return "§c呜...找不到名叫 '" + kitName + "' 的工具包的说喵~";
        }
        String permission = kitsConfig.getString(kitName + ".permission");
        if (permission != null && !player.hasPermission(permission)) {
            return "§c呜...主人没有权限领这个工具包的说~喵~";
        }
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

    public boolean giveKit(Player player, String kitName) {
        String canClaim = canClaimKit(player, kitName);
        if (canClaim != null) {
            player.sendMessage(canClaim);
            return false;
        }
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
        long cooldown = kitsConfig.getLong(kitName + ".cooldown", 0);
        if (cooldown > 0) {
            setLastClaimTime(player, kitName, System.currentTimeMillis());
        }
        player.sendMessage("§a呜呼~成功领到工具包 '§6" + kitName + "§a' 啦喵~");
        return true;
    }

    /**
     * 解析物品字符串为 ItemStack
     * 格式：MATERIAL:AMOUNT:ENCH:LVL,ENCH:LVL|name:xxx|lore:xxx|player:xxx|unbreakable|color:255,85,0
     * 兼容旧格式（无 | 分隔符）
     */
    private ItemStack parseItemString(String itemString) {
        try {
            String[] segments = itemString.split("\\|", -1);
            String baseString = segments[0];
            String[] baseParts = baseString.split(":");

            Material material = Material.valueOf(baseParts[0].toUpperCase(Locale.ENGLISH));
            int amount = baseParts.length > 1 ? Integer.parseInt(baseParts[1]) : 1;
            ItemStack item = new ItemStack(material, amount);

            // 附魔（baseParts[2] 格式：SHARPNESS:3,UNBREAKING:2）
            if (baseParts.length > 2) {
                for (String enchPart : baseParts[2].split(",")) {
                    String[] enchInfo = enchPart.split(":");
                    if (enchInfo.length >= 2) {
                        Enchantment ench = Enchantment.getByName(enchInfo[0].toUpperCase(Locale.ENGLISH));
                        if (ench != null) {
                            int level = Integer.parseInt(enchInfo[1]);
                            item.addUnsafeEnchantment(ench, level);
                        }
                    }
                }
            }

            // 应用元数据（从 | 分隔的段中解析）
            if (segments.length > 1) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    for (int i = 1; i < segments.length; i++) {
                        applyMeta(meta, item, segments[i]);
                    }
                    item.setItemMeta(meta);
                }
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("解析物品字符串失败：" + itemString);
            return null;
        }
    }

    /**
     * 应用单条元数据到 ItemMeta
     * 支持的标签：name, lore/desc, unbreakable, cmd/custom-model-data, itemflags,
     *            player/owner, author, title, page<N>, color/colour (皮革/烟花/药水),
     *            book, fade, shape/type, effect, power, amplifier, duration, splash,
     *            basecolor, trim, 以及附魔回退
     */
    private void applyMeta(ItemMeta meta, ItemStack item, String segment) {
        int colonIdx = segment.indexOf(':');
        String key;
        String value;
        if (colonIdx >= 0) {
            key = segment.substring(0, colonIdx).trim().toLowerCase(Locale.ENGLISH);
            value = segment.substring(colonIdx + 1).trim();
        } else {
            key = segment.trim().toLowerCase(Locale.ENGLISH);
            value = "";
        }

        try {
            switch (key) {
                case "name":
                    meta.setDisplayName(value.replace("_", " ").replace("&", "§"));
                    break;
                case "lore":
                case "desc":
                    List<String> lore = new ArrayList<>();
                    for (String line : value.split("\\|")) {
                        lore.add(line.replace("_", " ").replace("&", "§"));
                    }
                    meta.setLore(lore);
                    break;
                case "unbreakable":
                    meta.setUnbreakable(true);
                    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    break;
                case "cmd":
                case "custom-model-data":
                    try {
                        meta.setCustomModelData(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {}
                    break;
                case "itemflags":
                    for (String flag : value.split(",")) {
                        try {
                            meta.addItemFlags(ItemFlag.valueOf(flag.trim().toUpperCase(Locale.ENGLISH)));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    break;
                case "player":
                case "owner":
                    if (meta instanceof SkullMeta && item.getType() == Material.PLAYER_HEAD) {
                        SkullMeta skull = (SkullMeta) meta;
                        skull.setOwner(value);
                    }
                    break;
                case "author":
                    if (meta instanceof BookMeta) {
                        ((BookMeta) meta).setAuthor(value.replace("_", " "));
                    }
                    break;
                case "title":
                    if (meta instanceof BookMeta) {
                        ((BookMeta) meta).setTitle(value.replace("_", " ").replace("&", "§"));
                    }
                    break;
                case "book":
                    // book:<chapter> - 从 EssentialsX 章节文件加载，这里只记录名称
                    break;
                default:
                    // page<N>:<content> — 数字开头的 key
                    if (key.startsWith("page") && meta instanceof BookMeta) {
                        try {
                            int pageNum = Integer.parseInt(key.substring(4));
                            String[] lines = value.split("\\|");
                            StringBuilder pageContent = new StringBuilder();
                            for (int i = 0; i < lines.length; i++) {
                                if (i > 0) pageContent.append("\n");
                                pageContent.append(lines[i].replace("_", " ").replace("&", "§"));
                            }
                            BookMeta bookMeta = (BookMeta) meta;
                            List<String> pages = bookMeta.getPages();
                            while (pages.size() < pageNum) {
                                pages.add("");
                            }
                            pages.set(pageNum - 1, pageContent.toString());
                            bookMeta.setPages(pages);
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                    // 皮革/烟花颜色
                    if (key.equals("color") || key.equals("colour")) {
                        if (meta instanceof LeatherArmorMeta) {
                            ((LeatherArmorMeta) meta).setColor(parseColor(value));
                            meta.addItemFlags(ItemFlag.HIDE_DYE);
                        }
                        break;
                    }
                    // 烟花效果（分组格式：color:red,fade:green,shape:creeper,effect:twinkle）
                    if (key.equals("firework")) {
                        if (meta instanceof FireworkMeta) {
                            FireworkMeta fmeta = (FireworkMeta) meta;
                            String[] effects = value.split(";");
                            for (String effectStr : effects) {
                                String[] props = effectStr.split(",");
                                FireworkEffect.Builder builder = FireworkEffect.builder();
                                for (String prop : props) {
                                    int ci = prop.indexOf(':');
                                    if (ci <= 0) continue;
                                    String pKey = prop.substring(0, ci).trim().toLowerCase(Locale.ENGLISH);
                                    String pVal = prop.substring(ci + 1).trim();
                                    switch (pKey) {
                                        case "color":
                                        case "colour":
                                            for (String c : pVal.split(",")) {
                                                builder.withColor(parseColor(c.trim()));
                                            }
                                            break;
                                        case "fade":
                                            for (String c : pVal.split(",")) {
                                                builder.withFade(parseColor(c.trim()));
                                            }
                                            break;
                                        case "shape":
                                        case "type":
                                            try {
                                                builder.with(FireworkEffect.Type.valueOf(pVal.toUpperCase(Locale.ENGLISH)));
                                            } catch (IllegalArgumentException ignored) {}
                                            break;
                                        case "effect":
                                            for (String ef : pVal.split(",")) {
                                                if (ef.trim().equalsIgnoreCase("twinkle")) {
                                                    builder.flicker(true);
                                                }
                                                if (ef.trim().equalsIgnoreCase("trail")) {
                                                    builder.trail(true);
                                                }
                                            }
                                            break;
                                    }
                                }
                                fmeta.addEffect(builder.build());
                            }
                        }
                        break;
                    }
                    // 烟花飞行时间
                    if (key.equals("power")) {
                        if (meta instanceof FireworkMeta) {
                            try {
                                ((FireworkMeta) meta).setPower(Math.min(4, Math.max(0, Integer.parseInt(value))));
                            } catch (NumberFormatException ignored) {}
                        }
                        break;
                    }
                    // 旗帜 basecolor
                    if (key.equals("basecolor")) {
                        if (meta instanceof BlockStateMeta) {
                            BlockState state = ((BlockStateMeta) meta).getBlockState();
                            if (state instanceof Banner) {
                                ((Banner) state).setBaseColor(parseDyeColor(value));
                                ((BlockStateMeta) meta).setBlockState(state);
                            }
                        }
                        break;
                    }
                    // 护甲纹饰 trim
                    if (key.equals("trim")) {
                        break;
                    }
                    // 药水效果（分组格式：effect:SPEED,power:2,duration:60,splash:true）
                    if (key.equals("potion")) {
                        if (meta instanceof PotionMeta) {
                            PotionMeta potionMeta = (PotionMeta) meta;
                            PotionEffectType effectType = null;
                            int amplifier = 0;
                            int duration = 60; // 默认60秒
                            boolean isSplash = false;
                            
                            String[] props = value.split(",");
                            for (String prop : props) {
                                int ci = prop.indexOf(':');
                                if (ci <= 0) continue;
                                String pKey = prop.substring(0, ci).trim().toLowerCase(Locale.ENGLISH);
                                String pVal = prop.substring(ci + 1).trim();
                                switch (pKey) {
                                    case "effect":
                                        effectType = PotionEffectType.getByName(pVal.toUpperCase(Locale.ENGLISH));
                                        break;
                                    case "power":
                                    case "amplifier":
                                        try { amplifier = Integer.parseInt(pVal); } catch (NumberFormatException ignored) {}
                                        break;
                                    case "duration":
                                        try { duration = Integer.parseInt(pVal); } catch (NumberFormatException ignored) {}
                                        break;
                                    case "splash":
                                        isSplash = Boolean.parseBoolean(pVal);
                                        break;
                                }
                            }
                            if (effectType != null) {
                                potionMeta.addCustomEffect(new PotionEffect(effectType, duration * 20, amplifier), true);
                            }
                        }
                        break;
                    }
                    // 回退：尝试作为附魔处理
                    Enchantment ench = Enchantment.getByName(key.toUpperCase(Locale.ENGLISH));
                    if (ench != null) {
                        int lvl = ench.getMaxLevel();
                        if (!value.isEmpty()) {
                            try {
                                lvl = Integer.parseInt(value);
                            } catch (NumberFormatException ignored) {}
                        }
                        if (lvl == 0) {
                            item.removeEnchantment(ench);
                        } else {
                            item.addUnsafeEnchantment(ench, lvl);
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("应用物品元数据失败：" + segment + " (" + e.getMessage() + ")");
        }
    }

    /**
     * 解析颜色值（支持 RGB 整数 / #hex / R,G,B）
     */
    private Color parseColor(String value) {
        if (value.startsWith("#") && value.length() == 7) {
            try {
                int rgb = Integer.parseInt(value.substring(1), 16);
                return Color.fromRGB(rgb);
            } catch (NumberFormatException ignored) {}
        }
        String[] parts = value.split(",");
        if (parts.length == 3) {
            try {
                return Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException ignored) {}
        }
        try {
            return Color.fromRGB(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {}
        return Color.WHITE;
    }

    /**
     * 解析 DyeColor 名称
     */
    private DyeColor parseDyeColor(String value) {
        try {
            return DyeColor.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {}
        return DyeColor.WHITE;
    }

    private long getLastClaimTime(Player player, String kitName) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = kitCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        return playerCooldowns.getOrDefault(kitName, 0L);
    }

    private void setLastClaimTime(Player player, String kitName, long time) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = kitCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        playerCooldowns.put(kitName, time);
    }

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

    public void reload() {
        loadKits();
    }
}
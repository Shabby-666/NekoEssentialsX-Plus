package com.nekoessentialsx.catstyle;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.titles.TitleManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;

import java.util.regex.Pattern;

public class CatChatProcessor implements Listener {
    private static CatChatProcessor instance;
    private final NekoEssentialX plugin;
    private final CatStyleManager catStyleManager;
    private final BukkitAudiences audiences;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final java.util.Set<String> processedAdvancements = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private CatChatProcessor(NekoEssentialX plugin, CatStyleManager catStyleManager) {
        this.plugin = plugin;
        this.catStyleManager = catStyleManager;
        this.audiences = BukkitAudiences.create(plugin);
    }

    public static void init(NekoEssentialX plugin, CatStyleManager catStyleManager) {
        if (instance == null) {
            instance = new CatChatProcessor(plugin, catStyleManager);
            plugin.getServer().getPluginManager().registerEvents(instance, plugin);
        }
    }

    public static CatChatProcessor getInstance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // 转换聊天消息为猫娘风格，并添加喵~后缀
        message = convertToCatStyle(message);
        if (!message.endsWith("喵~")) {
            message = message + "喵~";
        }
        
        // 设置格式化的聊天消息：玩家名 >> 消息内容
        event.setFormat("§d%s §a>> §e" + message);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 转换玩家加入消息（兼容旧版本Spigot API）
        try {
            // 尝试使用旧版本API
            String original = event.getJoinMessage();
            if (original != null && !original.isEmpty()) {
                String processed = processMessage(original);
                event.setJoinMessage(processed);
            }
        } catch (NoSuchMethodError e) {
            // 忽略，继续执行
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 转换玩家退出消息（兼容旧版本Spigot API）
        try {
            // 尝试使用旧版本API
            String original = event.getQuitMessage();
            if (original != null && !original.isEmpty()) {
                String processed = processMessage(original);
                event.setQuitMessage(processed);
            }
        } catch (NoSuchMethodError e) {
            // 忽略，继续执行
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 转换玩家被踢消息（兼容旧版本Spigot API）
        try {
            // 尝试使用旧版本API
            String original = event.getLeaveMessage();
            if (original != null && !original.isEmpty()) {
                String processed = processMessage(original);
                event.setLeaveMessage(processed);
            }
            
            // 转换踢人原因
            String reason = event.getReason();
            if (reason != null && !reason.isEmpty()) {
                String processedReason = processMessage(reason);
                event.setReason(processedReason);
            }
        } catch (NoSuchMethodError e) {
            // 忽略，继续执行
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 记录命令，以便后续处理反馈消息
        String command = event.getMessage();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        // 可以在这里添加命令特定的处理
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onServerCommand(ServerCommandEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 服务器命令处理
        String command = event.getCommand();
        // 可以在这里添加服务器命令的反馈处理
    }
    
    /**
     * 拦截发送给玩家的所有消息，为系统消息添加喵~后缀
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAsyncChat(AsyncPlayerChatEvent event) {
        // 此方法仅用于确保聊天消息被处理（已在onPlayerChat中处理）
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        // 转换重生相关消息
        // 注意：PlayerRespawnEvent本身没有消息，但可能会有相关的系统消息
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!catStyleManager.isEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().toString();
        
        // 生成唯一标识符，防止同一成就多次触发
        String uniqueId = player.getUniqueId() + ":" + advancementKey;
        
        // 检查是否已经处理过该成就
        if (processedAdvancements.contains(uniqueId)) {
            return;
        }
        
        // 添加到已处理集合
        processedAdvancements.add(uniqueId);
        
        // 获取成就的显示名称（如果有），否则使用键的最后一部分
        String advancementName;
        
        // 尝试获取成就的显示名称
        try {
            // 对于原版Minecraft成就，我们可以从成就键中提取有意义的名称
            // 例如：minecraft:story/mine_diamond -> mine_diamond -> 挖掘钻石
            // 移除命名空间
            String simpleKey = advancementKey.substring(advancementKey.lastIndexOf(':') + 1);
            // 移除路径部分
            String finalKey = simpleKey.substring(simpleKey.lastIndexOf('/') + 1);
            // 将下划线替换为空格
            advancementName = finalKey.replace('_', ' ');
            // 首字母大写
            advancementName = advancementName.substring(0, 1).toUpperCase() + advancementName.substring(1);
        } catch (Exception e) {
            // 如果处理失败，使用完整键
            advancementName = advancementKey;
        }
        
        // 发送猫娘风格的成就提示
        // 直接使用Bukkit API发送消息，避免被其他处理器重复处理
        player.sendMessage("§a恭喜你获得了成就 §6" + advancementName + " §a喵~");
        
        // 5秒后从集合中移除，以便玩家可以再次获得同一成就时收到提示
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            processedAdvancements.remove(uniqueId);
        }, 100L);
    }
    
    // 注意：使用Bukkit的事件系统无法直接拦截所有服务器命令反馈消息
    // 但我们可以确保所有插件发送的消息都经过我们的处理
    // 对于原生命令消息，我们将在未来版本中通过PacketListener实现拦截
    
    // 添加一个方法来处理所有发送给玩家的消息
    public void sendCatStyleMessage(Player player, Component component) {
        if (catStyleManager.isEnabled()) {
            component = processComponent(component);
        }
        audiences.player(player).sendMessage(component);
    }
    
    public void sendCatStyleMessage(Player player, String message) {
        if (catStyleManager.isEnabled()) {
            message = processMessage(message);
        }
        player.sendMessage(message);
    }
    
    // 处理所有玩家命令反馈
    public void handleCommandFeedback(Player player, String message) {
        if (catStyleManager.isEnabled()) {
            message = processMessage(message);
            player.sendMessage(message);
        } else {
            player.sendMessage(message);
        }
    }

    public String convertToCatStyle(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 根据结城さくな（结城昨奈）的角色设定重写关键词替换
        // 害羞、猫娘女仆、有点冒失、怕生但真诚
        message = message.replaceAll(Pattern.quote("你好"), "那个...你好呀~")
                        .replaceAll(Pattern.quote("好的"), "好的喵~")
                        .replaceAll(Pattern.quote("谢谢"), "谢谢喵~那个...不客气的说~")
                        .replaceAll(Pattern.quote("再见"), "再见喵...下次见~")
                        .replaceAll(Pattern.quote("是的"), "是...是的喵~")
                        .replaceAll(Pattern.quote("不是"), "不...不是啦~")
                        .replaceAll(Pattern.quote("嗯"), "嗯...嗯喵~")
                        .replaceAll(Pattern.quote("哦"), "哦...这样呀~")
                        .replaceAll(Pattern.quote("啊"), "啊喵~")
                        .replaceAll(Pattern.quote("哇"), "哇~好厉害喵~")
                        .replaceAll(Pattern.quote("已"), "已经~")
                        .replaceAll(Pattern.quote("了"), "啦~")
                        .replaceAll(Pattern.quote("的"), "的喵~")
                        .replaceAll(Pattern.quote("请"), "请...请啦~")
                        .replaceAll(Pattern.quote("可以"), "可以吗？喵~")
                        .replaceAll(Pattern.quote("不"), "不...不要~")
                        .replaceAll(Pattern.quote("是"), "是喵~")
                        .replaceAll(Pattern.quote("在"), "在喵~")
                        .replaceAll(Pattern.quote("有"), "有喵~")
                        .replaceAll(Pattern.quote("没有"), "没...没有啦~")
                        .replaceAll(Pattern.quote("会"), "会的喵~")
                        .replaceAll(Pattern.quote("不会"), "不...不会的~")
                        .replaceAll(Pattern.quote("要"), "要...要的~")
                        .replaceAll(Pattern.quote("不要"), "不...不要啦~")
                        .replaceAll(Pattern.quote("需要"), "需要吗？喵~")
                        .replaceAll(Pattern.quote("不需要"), "不...不需要的~")
                        .replaceAll(Pattern.quote("必须"), "必须的喵~")
                        .replaceAll(Pattern.quote("应该"), "应该...是吧~")
                        .replaceAll(Pattern.quote("不应该"), "不...不应该啦~")
                        .replaceAll(Pattern.quote("能"), "能的喵~")
                        .replaceAll(Pattern.quote("不能"), "不...不能啦~")
                        .replaceAll(Pattern.quote("想"), "想...想的~")
                        .replaceAll(Pattern.quote("不想"), "不...不想啦~")
                        .replaceAll(Pattern.quote("喜欢"), "喜欢喵~")
                        .replaceAll(Pattern.quote("不喜欢"), "不...不喜欢的~")
                        .replaceAll(Pattern.quote("爱"), "爱喵~")
                        .replaceAll(Pattern.quote("不爱"), "不...不爱的~")
                        .replaceAll(Pattern.quote("恨"), "恨...恨啦~")
                        .replaceAll(Pattern.quote("不恨"), "不...不恨的~")
                        .replaceAll(Pattern.quote("知道"), "知道的喵~")
                        .replaceAll(Pattern.quote("不知道"), "不...不知道啦~")
                        .replaceAll(Pattern.quote("明白"), "明白啦~")
                        .replaceAll(Pattern.quote("不明白"), "不...不明白的说~")
                        .replaceAll(Pattern.quote("懂"), "懂的喵~")
                        .replaceAll(Pattern.quote("不懂"), "不...不懂啦~")
                        .replaceAll(Pattern.quote("做"), "做...做的~")
                        .replaceAll(Pattern.quote("不做"), "不...不做啦~")
                        .replaceAll(Pattern.quote("去"), "去...去的~")
                        .replaceAll(Pattern.quote("不去"), "不...不去啦~")
                        .replaceAll(Pattern.quote("来"), "来...来的~")
                        .replaceAll(Pattern.quote("不来"), "不...不来啦~")
                        .replaceAll(Pattern.quote("上"), "上...上的~")
                        .replaceAll(Pattern.quote("不上"), "不...不上啦~")
                        .replaceAll(Pattern.quote("下"), "下...下的~")
                        .replaceAll(Pattern.quote("不下"), "不...不下啦~")
                        .replaceAll(Pattern.quote("左"), "左...左边~")
                        .replaceAll(Pattern.quote("右"), "右...右边~")
                        // 添加一些冒失和害羞的表达
                        .replaceAll(Pattern.quote("对不起"), "对不起！我不是故意的喵~")
                        .replaceAll(Pattern.quote("抱歉"), "抱歉...我错了~")
                        .replaceAll(Pattern.quote("游戏"), "游戏！好喜欢喵~")
                        .replaceAll(Pattern.quote("唱歌"), "唱歌...我会努力的~")
                        .replaceAll(Pattern.quote("直播"), "直播...有点紧张喵~")
                        .replaceAll(Pattern.quote("粉丝"), "粉丝...谢谢大家~")
                        .replaceAll(Pattern.quote("互动"), "互动...我会加油的~");

        return message;
    }

    public String processMessage(String original) {
        if (!catStyleManager.isEnabled()) {
            return original;
        }

        String message = original;
        
        // 检查是否已经有喵~后缀
        if (message.endsWith("喵~")) {
            return message;
        }
        
        // 对系统消息（join/quit/death/命令输出）添加喵~后缀
        message = message + "§6喵~";
        
        return message;
    }
    
    /**
     * 处理插件消息（不重复添加后缀）
     */
    public String processPluginMessage(String original) {
        if (!catStyleManager.isEnabled()) {
            return original;
        }
        // 插件消息由调用者自行控制后缀，这里不做额外处理
        return original;
    }

    private String applyMessageReplacements(String message) {
        // 应用配置中的消息替换
        if (catStyleManager.getConfig().contains("message-replacements")) {
            // 定义原始消息到配置键的映射
            java.util.Map<String, String> messageKeyMap = new java.util.HashMap<>();
            messageKeyMap.put("Unknown command.", "unknown_command");
            messageKeyMap.put("You don't have permission to use this command.", "you_dont_have_permission");
            messageKeyMap.put("Please enter a valid number.", "please_enter_a_valid_number");
            messageKeyMap.put("Player not found.", "player_not_found");
            messageKeyMap.put("Teleport request sent.", "teleport_request_sent");
            messageKeyMap.put("Teleport request accepted.", "teleport_request_accepted");
            messageKeyMap.put("Teleport request denied.", "teleport_request_denied");
            messageKeyMap.put("Teleport request timed out.", "teleport_request_timed_out");
            
            // 遍历映射，检查并替换消息
            for (java.util.Map.Entry<String, String> entry : messageKeyMap.entrySet()) {
                String originalMessage = entry.getKey();
                String configKey = entry.getValue();
                String replacement = catStyleManager.getConfig().getString("message-replacements." + configKey);
                
                if (replacement != null && message.contains(originalMessage)) {
                    message = message.replace(originalMessage, replacement);
                }
            }
        }
        return message;
    }

    public Component processComponent(Component component) {
        if (!catStyleManager.isEnabled() || component == null) {
            return component;
        }

        // 我们需要特殊处理组件，确保保留其功能
        // 对于带有点击事件等高级功能的组件，我们不进行转换，只对普通文本消息进行转换
        return component;
    }
    
    // 为TPA等特殊消息提供专门的猫娘风格处理方法
    public String processSimpleMessage(String original) {
        if (!catStyleManager.isEnabled()) {
            return original;
        }

        String message = original;
        
        // 检查是否需要替换这个消息
        message = applyMessageReplacements(message);
        
        // 转换为猫娘风格
        message = convertToCatStyle(message);
        
        return message;
    }
}
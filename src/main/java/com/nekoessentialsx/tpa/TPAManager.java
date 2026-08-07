package com.nekoessentialsx.tpa;

import com.nekoessentialsx.NekoEssentialX;
import com.nekoessentialsx.catstyle.CatChatProcessor;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPAManager {
    private final NekoEssentialX plugin;
    private final Map<UUID, TPARequest> tpaRequests = new HashMap<>();
    private final BukkitAudiences audiences;
    private final MiniMessage miniMessage;
    
    // TPA请求超时时间（秒）
    private final int REQUEST_TIMEOUT = 30;
    
    public TPAManager(NekoEssentialX plugin) {
        this.plugin = plugin;
        this.audiences = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * 发送TPA请求
     * @param sender 发送者
     * @param target 目标玩家
     * @return 是否发送成功
     */
    public boolean sendRequest(Player sender, Player target) {
        // 检查是否已经存在请求
        for (TPARequest request : tpaRequests.values()) {
            if (request.getSender().getUniqueId().equals(sender.getUniqueId()) && 
                request.getTarget().getUniqueId().equals(target.getUniqueId())) {
                Component message = miniMessage.deserialize("<red>呜...你已经向这个杂鱼发过传送请求啦，别急啦喵~");
                // 使用猫娘风格处理消息
                CatChatProcessor catChatProcessor = CatChatProcessor.getInstance();
                if (catChatProcessor != null) {
                    message = catChatProcessor.processComponent(message);
                }
                audiences.player(sender).sendMessage(message);
                return false;
            }
        }
        
        // 创建新的TPA请求
        TPARequest request = new TPARequest(sender, target);
        tpaRequests.put(request.getId(), request);
        
        // 发送通知给发送者
        Component senderMessage = miniMessage.deserialize(
            "<green>传送请求已经送给 <yellow><name></yellow> 啦！等他的回应的说~喵~",
            Placeholder.component("name", Component.text(target.getName()))
        );
        // 使用猫娘风格处理消息
        CatChatProcessor catChatProcessor = CatChatProcessor.getInstance();
        if (catChatProcessor != null) {
            senderMessage = catChatProcessor.processComponent(senderMessage);
        }
        audiences.player(sender).sendMessage(senderMessage);
        
        // 发送带有可点击按钮的通知给目标
        String senderName = sender.getName();
        Component targetMessage = miniMessage.deserialize(
            "<yellow>" + senderName + "</yellow> <green>想传送到你这里来黏你的喵~ " +
            "<click:run_command:/tpaccept " + senderName + ">[<green>接受</green>]</click> " +
            "<click:run_command:/tpdeny " + senderName + ">[<red>拒绝</red>]</click>"
        );
        audiences.player(target).sendMessage(targetMessage);
        
        // 设置请求超时
        new BukkitRunnable() {
            @Override
            public void run() {
                if (tpaRequests.containsKey(request.getId())) {
                    tpaRequests.remove(request.getId());
                    Component timeoutSenderMessage = miniMessage.deserialize("<red>呜...你的传送请求超时啦，被人放鸽子了的说喵~");
                    Component timeoutTargetMessage = miniMessage.deserialize(
                        "<red><sender_name> <red>的传送请求超时了，没及时回就过期了的说喵~",
                        Placeholder.component("sender_name", Component.text(sender.getName()))
                    );
                    
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        timeoutSenderMessage = processor.processComponent(timeoutSenderMessage);
                        timeoutTargetMessage = processor.processComponent(timeoutTargetMessage);
                    }
                    audiences.player(sender).sendMessage(timeoutSenderMessage);
                    audiences.player(target).sendMessage(timeoutTargetMessage);
                }
            }
        }.runTaskLater(plugin, REQUEST_TIMEOUT * 20L);
        
        return true;
    }
    
    /**
     * 接受TPA请求
     * @param target 接收请求的玩家
     * @param senderName 发送请求的玩家名（可选，为null则接受所有）
     * @return 是否接受成功
     */
    public boolean acceptRequest(Player target, String senderName) {
        boolean accepted = false;
        
        if (senderName == null || senderName.equals("*")) {
            // 接受所有请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getTarget().getUniqueId().equals(target.getUniqueId())) {
                    executeTeleport(request);
                    accepted = true;
                }
            }
        } else {
            // 接受特定玩家的请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getTarget().getUniqueId().equals(target.getUniqueId()) && 
                    request.getSender().getName().equalsIgnoreCase(senderName)) {
                    executeTeleport(request);
                    accepted = true;
                    break;
                }
            }
            
            if (!accepted) {
                Component message = miniMessage.deserialize(
                    "<red>呜...找不到来自 <yellow><sender_name></yellow> <red>的传送请求的说~喵~",
                    Placeholder.component("sender_name", Component.text(senderName))
                );
                // 使用猫娘风格处理消息
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    message = processor.processComponent(message);
                }
                audiences.player(target).sendMessage(message);
            }
        }
        
        return accepted;
    }
    
    /**
     * 拒绝TPA请求
     * @param target 接收请求的玩家
     * @param senderName 发送请求的玩家名（可选，为null则拒绝所有）
     * @return 是否拒绝成功
     */
    public boolean denyRequest(Player target, String senderName) {
        boolean denied = false;
        
        if (senderName == null || senderName.equals("*")) {
            // 拒绝所有请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getTarget().getUniqueId().equals(target.getUniqueId())) {
                    Component senderMessage = miniMessage.deserialize(
                        "<red>呜...你的传送请求被 <yellow><target_name></yellow> <red>拒绝啦，好伤心的说~喵呜~",
                        Placeholder.component("target_name", Component.text(target.getName()))
                    );
                    Component targetMessage = miniMessage.deserialize(
                        "<green>哼~拒绝掉 <yellow><sender_name></yellow> <green>的传送请求啦，做得好喵~",
                        Placeholder.component("sender_name", Component.text(request.getSender().getName()))
                    );
                    
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        senderMessage = processor.processComponent(senderMessage);
                        targetMessage = processor.processComponent(targetMessage);
                    }
                    
                    audiences.player(request.getSender()).sendMessage(senderMessage);
                    audiences.player(target).sendMessage(targetMessage);
                    tpaRequests.remove(request.getId());
                    denied = true;
                }
            }
        } else {
            // 拒绝特定玩家的请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getTarget().getUniqueId().equals(target.getUniqueId()) && 
                    request.getSender().getName().equalsIgnoreCase(senderName)) {
                    Component senderMessage = miniMessage.deserialize(
                        "<red>呜...你的传送请求被 <yellow><target_name></yellow> <red>拒绝啦，好伤心的说~喵呜~",
                        Placeholder.component("target_name", Component.text(target.getName()))
                    );
                    Component targetMessage = miniMessage.deserialize(
                        "<green>哼~拒绝掉 <yellow><sender_name></yellow> <green>的传送请求啦，做得好喵~",
                        Placeholder.component("sender_name", Component.text(request.getSender().getName()))
                    );
                    
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        senderMessage = processor.processComponent(senderMessage);
                        targetMessage = processor.processComponent(targetMessage);
                    }
                    
                    audiences.player(request.getSender()).sendMessage(senderMessage);
                    audiences.player(target).sendMessage(targetMessage);
                    tpaRequests.remove(request.getId());
                    denied = true;
                    break;
                }
            }
            
            if (!denied) {
                Component message = miniMessage.deserialize(
                    "<red>呜...找不到来自 <yellow><sender_name></yellow> <red>的传送请求的说~喵~",
                    Placeholder.component("sender_name", Component.text(senderName))
                );
                // 使用猫娘风格处理消息
                CatChatProcessor processor = CatChatProcessor.getInstance();
                if (processor != null) {
                    message = processor.processComponent(message);
                }
                audiences.player(target).sendMessage(message);
            }
        }
        
        return denied;
    }
    
    /**
     * 取消TPA请求
     * @param sender 发送请求的玩家
     * @param targetName 目标玩家名（可选，为null则取消所有）
     * @return 是否取消成功
     */
    public boolean cancelRequest(Player sender, String targetName) {
        boolean cancelled = false;
        
        if (targetName == null) {
            // 取消所有请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getSender().getUniqueId().equals(sender.getUniqueId())) {
                    Component message = miniMessage.deserialize(
                        "<red><sender_name> <red>取消了传送请求的说，不来黏你啦喵~",
                        Placeholder.component("sender_name", Component.text(sender.getName()))
                    );
                    
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        message = processor.processComponent(message);
                    }
                    
                    audiences.player(request.getTarget()).sendMessage(message);
                    tpaRequests.remove(request.getId());
                    cancelled = true;
                }
            }
        } else {
            // 取消特定玩家的请求
            for (TPARequest request : new HashMap<>(tpaRequests).values()) {
                if (request.getSender().getUniqueId().equals(sender.getUniqueId()) && 
                    request.getTarget().getName().equalsIgnoreCase(targetName)) {
                    Component message = miniMessage.deserialize(
                        "<red><sender_name> <red>取消了传送请求的说，不来黏你啦喵~",
                        Placeholder.component("sender_name", Component.text(sender.getName()))
                    );
                    
                    // 使用猫娘风格处理消息
                    CatChatProcessor processor = CatChatProcessor.getInstance();
                    if (processor != null) {
                        message = processor.processComponent(message);
                    }
                    
                    audiences.player(request.getTarget()).sendMessage(message);
                    tpaRequests.remove(request.getId());
                    cancelled = true;
                    break;
                }
            }
        }
        
        if (cancelled) {
            Component message = miniMessage.deserialize("<green>呜呼~传送请求已经取消啦喵~");
            // 使用猫娘风格处理消息
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                message = processor.processComponent(message);
            }
            audiences.player(sender).sendMessage(message);
        } else {
            Component message = miniMessage.deserialize("<red>呜...找不到对应的传送请求的说~喵~");
            // 使用猫娘风格处理消息
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                message = processor.processComponent(message);
            }
            audiences.player(sender).sendMessage(message);
        }
        
        return cancelled;
    }
    
    /**
     * 执行传送
     * @param request TPA请求
     */
    private void executeTeleport(TPARequest request) {
        Player sender = request.getSender();
        Player target = request.getTarget();
        
        // 使用延迟传送，避免"moved too quickly"警告
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 执行传送
            sender.teleport(target.getLocation());
            
            // 发送通知
            Component senderMessage = miniMessage.deserialize(
                "<green>传送成功啦！蹭蹭飞过去黏着杂鱼 <yellow><target_name></yellow> 的说~喵~",
                Placeholder.component("target_name", Component.text(target.getName()))
            );
            Component targetMessage = miniMessage.deserialize(
                "<green>接住了 <yellow><sender_name></yellow> <green>的传送请求！他过来啦的说~喵~",
                Placeholder.component("sender_name", Component.text(sender.getName()))
            );
            
            // 使用猫娘风格处理消息
            CatChatProcessor processor = CatChatProcessor.getInstance();
            if (processor != null) {
                senderMessage = processor.processComponent(senderMessage);
                targetMessage = processor.processComponent(targetMessage);
            }
            
            audiences.player(sender).sendMessage(senderMessage);
            audiences.player(target).sendMessage(targetMessage);
        }, 1L);
        
        // 移除请求
        tpaRequests.remove(request.getId());
    }
    
    /**
     * 获取玩家收到的所有TPA请求
     * @param player 玩家
     * @return TPA请求列表
     */
    public java.util.List<TPARequest> getReceivedRequests(Player player) {
        java.util.List<TPARequest> requests = new java.util.ArrayList<>();
        for (TPARequest request : tpaRequests.values()) {
            if (request.getTarget().getUniqueId().equals(player.getUniqueId())) {
                requests.add(request);
            }
        }
        return requests;
    }
    
    /**
     * 获取玩家发送的所有TPA请求
     * @param player 玩家
     * @return TPA请求列表
     */
    public java.util.List<TPARequest> getSentRequests(Player player) {
        java.util.List<TPARequest> requests = new java.util.ArrayList<>();
        for (TPARequest request : tpaRequests.values()) {
            if (request.getSender().getUniqueId().equals(player.getUniqueId())) {
                requests.add(request);
            }
        }
        return requests;
    }
    
    /**
     * TPA请求类
     */
    public static class TPARequest {
        private final UUID id;
        private final Player sender;
        private final Player target;
        private final long timestamp;
        
        public TPARequest(Player sender, Player target) {
            this.id = UUID.randomUUID();
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }
        
        public UUID getId() {
            return id;
        }
        
        public Player getSender() {
            return sender;
        }
        
        public Player getTarget() {
            return target;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
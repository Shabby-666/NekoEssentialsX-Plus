package com.nekoessentialsx.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CatCommandSender implements CommandSender {
    private final CommandSender original;
    
    public CatCommandSender(CommandSender original) {
        this.original = original;
    }
    
    @Override
    public void sendMessage(@NotNull String message) {
        // 在消息末尾添加"喵~"，如果已经有了就不再添加
        if (!message.endsWith("喵~") && !message.endsWith("~") && !message.endsWith("~\n")) {
            message += "喵~";
        }
        original.sendMessage(message);
    }
    
    @Override
    public void sendMessage(@NotNull String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            if (!messages[i].endsWith("喵~") && !messages[i].endsWith("~") && !messages[i].endsWith("~\n")) {
                messages[i] += "喵~";
            }
        }
        original.sendMessage(messages);
    }
    
    @Override
    public @NotNull String getName() {
        return original.getName();
    }
    
    @Override
    public @NotNull org.bukkit.permissions.PermissionAttachment addAttachment(@NotNull org.bukkit.plugin.Plugin plugin, @NotNull String name, boolean value) {
        return original.addAttachment(plugin, name, value);
    }
    
    @Override
    public @NotNull org.bukkit.permissions.PermissionAttachment addAttachment(@NotNull org.bukkit.plugin.Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return original.addAttachment(plugin, name, value, ticks);
    }
    
    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(@NotNull org.bukkit.plugin.Plugin plugin, int ticks) {
        return original.addAttachment(plugin, ticks);
    }
    
    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(@NotNull org.bukkit.plugin.Plugin plugin) {
        return original.addAttachment(plugin);
    }
    
    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return original.getEffectivePermissions();
    }
    
    @Override
    public boolean hasPermission(@NotNull String name) {
        return original.hasPermission(name);
    }
    
    @Override
    public boolean hasPermission(@NotNull org.bukkit.permissions.Permission perm) {
        return original.hasPermission(perm);
    }
    
    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return original.isPermissionSet(name);
    }
    
    @Override
    public boolean isPermissionSet(@NotNull org.bukkit.permissions.Permission perm) {
        return original.isPermissionSet(perm);
    }
    
    @Override
    public void recalculatePermissions() {
        original.recalculatePermissions();
    }
    
    @Override
    public void removeAttachment(@NotNull org.bukkit.permissions.PermissionAttachment attachment) {
        original.removeAttachment(attachment);
    }
    
    @Override
    public boolean isOp() {
        return original.isOp();
    }
    
    @Override
    public void setOp(boolean value) {
        original.setOp(value);
    }
    
    @Override
    public org.bukkit.Server getServer() {
        return original.getServer();
    }
    
    @Override
    public org.bukkit.command.CommandSender.Spigot spigot() {
        return original.spigot();
    }
    
    @Override
    public void sendMessage(java.util.UUID uuid, java.lang.String... messages) {
        original.sendMessage(uuid, messages);
    }
    
    @Override
    public void sendMessage(java.util.UUID uuid, java.lang.String message) {
        original.sendMessage(uuid, message);
    }
    
    // 辅助方法
    public CommandSender getOriginal() {
        return original;
    }
    
    public boolean isPlayer() {
        return original instanceof Player;
    }
    
    public boolean isConsole() {
        return original instanceof ConsoleCommandSender || original instanceof RemoteConsoleCommandSender;
    }
    
    public boolean isProxied() {
        return original instanceof ProxiedCommandSender;
    }
}
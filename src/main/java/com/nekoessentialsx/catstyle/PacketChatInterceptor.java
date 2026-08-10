package com.nekoessentialsx.catstyle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.nekoessentialsx.NekoEssentialX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Method;

public class PacketChatInterceptor {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    private static boolean nmsReady = false;
    private static Method nmsGetString;
    private static Method nmsLiteral;
    private static Method nmsEmpty;
    private static Method nmsAppend;
    private static Class<?> nmsComponent;

    public static void register(NekoEssentialX plugin, CatStyleManager catStyleManager) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        try {
            ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
            nmsComponent = serverLoader.loadClass("net.minecraft.network.chat.Component");
            nmsGetString = nmsComponent.getMethod("getString");
            nmsLiteral = nmsComponent.getMethod("literal", String.class);
            nmsEmpty = nmsComponent.getMethod("empty");

            plugin.getLogger().info("[NekoCat] Component methods:");
            for (java.lang.reflect.Method m : nmsComponent.getMethods()) {
                if (m.getDeclaringClass() == nmsComponent) {
                    plugin.getLogger().info("[NekoCat]   " + m);
                }
            }

            Class<?> mutableClass = serverLoader.loadClass("net.minecraft.network.chat.MutableComponent");
            plugin.getLogger().info("[NekoCat] MutableComponent methods:");
            for (java.lang.reflect.Method m : mutableClass.getMethods()) {
                if (m.getName().equals("append")) {
                    plugin.getLogger().info("[NekoCat]   APPEND: " + m);
                }
            }
            nmsAppend = mutableClass.getMethod("append", nmsComponent);
            nmsReady = true;
            plugin.getLogger().info("[NekoCat] NMS 反射成功!");
        } catch (Exception e) {
            plugin.getLogger().warning("[NekoCat] NMS 反射失败: " + e);
            nmsReady = false;
        }

        protocolManager.addPacketListener(new PacketAdapter(plugin,
                PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!catStyleManager.isEnabled()) return;

                try {
                    Object rawMessage = event.getPacket().getModifier().read(0);
                    if (rawMessage == null) return;

                    if (nmsReady) {
                        String text = (String) nmsGetString.invoke(rawMessage);
                        if (text == null || text.isEmpty()) return;
                        if (text.contains("喵")) return;

                        Object empty = nmsEmpty.invoke(null);
                        Object combined = nmsAppend.invoke(empty, rawMessage);
                        Object suffix = nmsLiteral.invoke(null, "喵~");
                        combined = nmsAppend.invoke(combined, suffix);
                        event.getPacket().getModifier().write(0, combined);
                    } else {
                        String text = rawMessage.toString();
                        if (text == null || text.isEmpty()) return;
                        if (text.contains("喵")) return;
                        Component modified = SERIALIZER.deserialize(text + "喵~");
                        event.getPacket().getModifier().write(0, modified);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[NekoCat] 消息拦截异常: " + e.getMessage());
                }
            }
        });

        plugin.getLogger().info("[NekoCat] ProtocolLib 消息拦截器已注册喵~");
    }
}

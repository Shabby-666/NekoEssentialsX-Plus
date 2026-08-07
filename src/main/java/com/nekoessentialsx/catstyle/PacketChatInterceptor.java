package com.nekoessentialsx.catstyle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.nekoessentialsx.NekoEssentialX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 基于 ProtocolLib 的包级拦截器。
 *
 * <p>拦截所有发往玩家的 {@code ClientboundSystemChatPacket}（系统聊天包），
 * 在文本末尾追加「喵~」后缀。覆盖 MC 所有内置消息：死亡、命令输出、加入、退出等。</p>
 */
public class PacketChatInterceptor {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    public static void register(NekoEssentialX plugin, CatStyleManager catStyleManager) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!catStyleManager.isEnabled()) return;

                Object rawMessage = event.getPacket().getModifier().read(0);
                if (!(rawMessage instanceof Component adventureComponent)) return;

                String text = SERIALIZER.serialize(adventureComponent);
                if (text == null || text.isEmpty() || text.endsWith("喵~")) return;

                Component modified = SERIALIZER.deserialize(text + "§6喵~");
                event.getPacket().getModifier().write(0, modified);
            }
        });

        plugin.getLogger().info("[NekoCat] ProtocolLib 消息拦截器已注册");
    }
}

package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class ConfigContentPacket {
    private final String needId;
    private final String content;

    public ConfigContentPacket(final String needId, final String content) {
        this.needId = needId;
        this.content = content;
    }

    public static void encode(final ConfigContentPacket msg, final PacketBuffer packetBuffer) {
        packetBuffer.writeString(msg.needId);
        packetBuffer.writeString(msg.content);
    }

    public static ConfigContentPacket decode(final PacketBuffer packetBuffer) {
        return new ConfigContentPacket(packetBuffer.readString(), packetBuffer.readString());
    }

    public static void handle(final ConfigContentPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NeedRegistry.INSTANCE.updateFromRemote(msg.needId, msg.content);
        });
        ctx.get().setPacketHandled(true);
    }
}

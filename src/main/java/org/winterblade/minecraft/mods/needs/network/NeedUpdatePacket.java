package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.function.Supplier;

public class NeedUpdatePacket {
    private final String need;
    private final double value;
    private final double min;
    private final double max;

    public NeedUpdatePacket(final String need, final double value, final double min, final double max) {
        this.need = need;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public void encode(final PacketBuffer packetBuffer) {
        packetBuffer.writeString(need);
        packetBuffer.writeDouble(value);
        packetBuffer.writeDouble(min);
        packetBuffer.writeDouble(max);
    }

    public static NeedUpdatePacket decode(final PacketBuffer packetBuffer) {
        return new NeedUpdatePacket(packetBuffer.readString(), packetBuffer.readDouble(), packetBuffer.readDouble(), packetBuffer.readDouble());
    }

    public static void handle(final NeedUpdatePacket msg, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NeedRegistry.INSTANCE.setLocalNeed(msg.need, msg.value, msg.min, msg.max);
        });

        ctx.get().setPacketHandled(true);
    }
}

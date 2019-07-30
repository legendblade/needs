package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.function.Supplier;

public class NeedUpdatePacket {
    private String need;

    private double value;

    public NeedUpdatePacket(String need, double value) {
        this.need = need;
        this.value = value;
    }

    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeString(need);
        packetBuffer.writeDouble(value);
    }

    public static NeedUpdatePacket decode(PacketBuffer packetBuffer) {
        return new NeedUpdatePacket(packetBuffer.readString(), packetBuffer.readDouble());
    }

    public static void handle(NeedUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NeedRegistry.INSTANCE.setLocalNeed(msg.need, msg.value);
        });

        ctx.get().setPacketHandled(true);
    }
}

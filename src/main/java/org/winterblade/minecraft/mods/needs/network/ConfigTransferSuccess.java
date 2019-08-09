package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class ConfigTransferSuccess {

    @SuppressWarnings("unused")
    public static void encode(final ConfigTransferSuccess msg, final PacketBuffer packetBuffer) {
        // Yep.
    }

    @SuppressWarnings("unused")
    public static ConfigTransferSuccess decode(final PacketBuffer packetBuffer) {
        return new ConfigTransferSuccess();
    }

    @SuppressWarnings("unused")
    public static void handle(final ConfigTransferSuccess msg, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NetworkManager.processQueue(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }
}

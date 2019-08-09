package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class ConfigTransferFailure {

    private final String needId;

    public ConfigTransferFailure(final String needId) {
        this.needId = needId;
    }

    public static void encode(final ConfigTransferFailure msg, final PacketBuffer packetBuffer) {
        packetBuffer.writeString(msg.needId);
    }

    public static ConfigTransferFailure decode(final PacketBuffer packetBuffer) {
        return new ConfigTransferFailure(packetBuffer.readString());
    }

    public static void handle(final ConfigTransferFailure msg, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ServerPlayerEntity player = ctx.get().getSender();

            if (player == null) return;

            NeedsMod.LOGGER.warn("Player " + player.getScoreboardName() + " unable to sync config " + msg.needId);
            player.connection.disconnect(new StringTextComponent("You were unable to receive a config from the server."));
        });
        ctx.get().setPacketHandled(true);
    }
}

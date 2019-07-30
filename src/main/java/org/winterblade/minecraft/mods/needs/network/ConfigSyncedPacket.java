package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.function.Supplier;

/**
 * Used to inform the server that we're done syncing configs
 */
@SuppressWarnings("unused")
public class ConfigSyncedPacket {
    public void encode(PacketBuffer packetBuffer) {
    }

    public static ConfigSyncedPacket decode(PacketBuffer packetBuffer) {
        return new ConfigSyncedPacket();
    }

    public static void handle(ConfigSyncedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.get().getSender();
            if (sender == null) return;

            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> sender);
            NeedRegistry.INSTANCE.onConfigSynced(sender, (n, v) ->
                NetworkManager.INSTANCE.send(target, new NeedUpdatePacket(n.getName(), v))
            );
        });
        ctx.get().setPacketHandled(true);
    }
}

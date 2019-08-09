package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.WeakHashMap;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = NeedsMod.VERSION;
    private static final WeakHashMap<ServerPlayerEntity, Queue<ConfigContentPacket>> contentQueue = new WeakHashMap<>();

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NeedsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals, // TODO: Probably should let same versions match each other
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        if (0 < packetId) return;

        INSTANCE.registerMessage(packetId++, ConfigCheckPacket.class, ConfigCheckPacket::encode, ConfigCheckPacket::decode, ConfigCheckPacket::handle);
        INSTANCE.registerMessage(packetId++, ConfigDesyncPacket.class, ConfigDesyncPacket::encode, ConfigDesyncPacket::decode, ConfigDesyncPacket::handle);
        INSTANCE.registerMessage(packetId++, ConfigContentPacket.class, ConfigContentPacket::encode, ConfigContentPacket::decode, ConfigContentPacket::handle);
        INSTANCE.registerMessage(packetId++, ConfigTransferSuccess.class, ConfigTransferSuccess::encode, ConfigTransferSuccess::decode, ConfigTransferSuccess::handle);
        INSTANCE.registerMessage(packetId++, ConfigTransferFailure.class, ConfigTransferFailure::encode, ConfigTransferFailure::decode, ConfigTransferFailure::handle);
        INSTANCE.registerMessage(packetId++, ConfigSyncedPacket.class, ConfigSyncedPacket::encode, ConfigSyncedPacket::decode, ConfigSyncedPacket::handle);
        INSTANCE.registerMessage(packetId++, NeedUpdatePacket.class, NeedUpdatePacket::encode, NeedUpdatePacket::decode, NeedUpdatePacket::handle);
    }

    /**
     * Adds a config to the queue for the player to receive.
     * @param player The player
     * @param packet The config packet
     */
    static void queueConfigContent(final ServerPlayerEntity player, final ConfigContentPacket packet) {
        contentQueue.computeIfAbsent(player, (p) -> new LinkedList<>()).add(packet);
    }


    static void processQueue(@Nullable final ServerPlayerEntity player) {
        if (player == null) return; // When ctx.get().getSender() returns null.

        final Queue<ConfigContentPacket> queue = contentQueue.get(player);
        if (queue == null) return; // If we have no packets to send.

        // Once we're done processing, clean up after ourselves
        final PacketDistributor.PacketTarget pd = PacketDistributor.PLAYER.with(() -> player);
        if (queue.isEmpty()) {
            contentQueue.remove(player);
            NeedRegistry.INSTANCE.sendConfigHashesToPlayer(player); // Resend the hashes
            return;
        }

        // Send the packet off
        INSTANCE.send(pd, queue.poll());
    }
}
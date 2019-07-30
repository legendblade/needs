package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.winterblade.minecraft.mods.needs.NeedsMod;

public class NetworkManager {
    private static String PROTOCOL_VERSION = NeedsMod.VERSION;

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
        // TODO: Config sync packet

        INSTANCE.registerMessage(packetId++, NeedUpdatePacket.class, NeedUpdatePacket::encode, NeedUpdatePacket::decode, NeedUpdatePacket::handle);
    }
}
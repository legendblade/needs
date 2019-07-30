package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigCheckPacket {
    private final Map<String, byte[]> configHashes;

    public ConfigCheckPacket(Map<String, byte[]> configHashes) {
        this.configHashes = configHashes;
    }

    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeInt(configHashes.size());
        configHashes.forEach((k, v) -> {
            packetBuffer.writeString(k);
            packetBuffer.writeByteArray(v);
        });
    }

    public static ConfigCheckPacket decode(PacketBuffer packetBuffer) {
        Map<String, byte[]> output = new HashMap<>();

        int count = packetBuffer.readInt();
        for (int i = 0; i < count; i++) {
            output.put(packetBuffer.readString(), packetBuffer.readByteArray());
        }

        return new ConfigCheckPacket(output);
    }

    public static void handle(ConfigCheckPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            NeedRegistry.INSTANCE.validateConfig(msg.configHashes);
        });
        ctx.get().setPacketHandled(true);
    }
}

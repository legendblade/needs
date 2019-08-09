package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigDesyncPacket {
    private final static DesyncTypes[] desyncTypeCache = DesyncTypes.values();
    private final Map<String, DesyncTypes> desyncs;

    public ConfigDesyncPacket(final Map<String,DesyncTypes> desyncs) {
        this.desyncs = desyncs;
    }

    public void encode(final PacketBuffer packetBuffer) {
        packetBuffer.writeInt(desyncs.size());
        desyncs.forEach((k, v) -> {
            packetBuffer.writeString(k);
            packetBuffer.writeInt(v.ordinal());
        });
    }

    public static ConfigDesyncPacket decode(final PacketBuffer packetBuffer) {
        final int count = packetBuffer.readInt();
        final Map<String, DesyncTypes> output = new HashMap<>();

        for (int i = 0; i < count; i++) {
            output.put(packetBuffer.readString(), desyncTypeCache[packetBuffer.readInt()]);
        }

        return new ConfigDesyncPacket(output);
    }

    public static void handle(final ConfigDesyncPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            final ServerPlayerEntity thePlayer = ctx.get().getSender();

            if (thePlayer == null) return;

            msg.desyncs.forEach((needId, desyncType) -> {
                NetworkManager.queueConfigContent(thePlayer, new ConfigContentPacket(needId, NeedRegistry.INSTANCE.getContent(needId)));

                // Let the player know
                if (desyncType == DesyncTypes.MISSING) {
                    thePlayer.sendMessage(new StringTextComponent("Need " + needId + " does not exist in your local config."));
                    return;
                }

                thePlayer.sendMessage(new StringTextComponent("Need " + needId + "'s config does not match between server and client."));
            });

            NetworkManager.processQueue(thePlayer);
        });
        ctx.get().setPacketHandled(true);
    }

    public enum DesyncTypes {
        MISSING,
        BAD_HASH
    }
}

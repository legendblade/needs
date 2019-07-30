package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigDesyncPacket {
    private final static DesyncTypes[] desyncTypeCache = DesyncTypes.values();
    private final Map<String, DesyncTypes> desyncs;

    public ConfigDesyncPacket(Map<String,DesyncTypes> desyncs) {
        this.desyncs = desyncs;
    }

    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeInt(desyncs.size());
        desyncs.forEach((k, v) -> {
            packetBuffer.writeString(k);
            packetBuffer.writeInt(v.ordinal());
        });
    }

    public static ConfigDesyncPacket decode(PacketBuffer packetBuffer) {
        int count = packetBuffer.readInt();
        Map<String, DesyncTypes> output = new HashMap<>();

        for (int i = 0; i < count; i++) {
            output.put(packetBuffer.readString(), desyncTypeCache[packetBuffer.readInt()]);
        }

        return new ConfigDesyncPacket(output);
    }

    public static void handle(ConfigDesyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity thePlayer = ctx.get().getSender();

            if (thePlayer == null) return;

            // TODO: deal with updating configs
            msg.desyncs.forEach((needId, desyncType) -> {
                if (desyncType == DesyncTypes.MISSING) {
                    thePlayer.sendMessage(new StringTextComponent("Need " + needId + " does not exist in your local config."));
                    return;
                }

                thePlayer.sendMessage(new StringTextComponent("Need " + needId + "'s config does not match between server and client."));
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public enum DesyncTypes {
        MISSING,
        BAD_HASH
    }
}

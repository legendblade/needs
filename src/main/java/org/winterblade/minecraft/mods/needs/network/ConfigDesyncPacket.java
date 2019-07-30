package org.winterblade.minecraft.mods.needs.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigDesyncPacket {
    private final static desyncTypes[] desyncTypeCache = desyncTypes.values();
    private final String needId;
    private final desyncTypes desyncType;

    public ConfigDesyncPacket(String needId, desyncTypes desyncType) {
        this.needId = needId;
        this.desyncType = desyncType;
    }

    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeString(needId);
        packetBuffer.writeInt(desyncType.ordinal());
    }

    public static ConfigDesyncPacket decode(PacketBuffer packetBuffer) {
        return new ConfigDesyncPacket(packetBuffer.readString(), desyncTypeCache[packetBuffer.readInt()]);
    }

    public static void handle(ConfigDesyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity thePlayer = ctx.get().getSender();

            if (thePlayer == null) return;

            // TODO: deal with updating configs
            if (msg.desyncType == desyncTypes.MISSING) {
                thePlayer.sendMessage(new StringTextComponent("Need " + msg.needId + " does not exist in your local config."));
                return;
            }

            thePlayer.sendMessage(new StringTextComponent("Need " + msg.needId + "'s config does not match between server and client."));
        });
        ctx.get().setPacketHandled(true);
    }

    public enum desyncTypes {
        MISSING,
        BAD_HASH
    }
}

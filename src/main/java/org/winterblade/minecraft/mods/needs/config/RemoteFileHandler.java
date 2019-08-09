package org.winterblade.minecraft.mods.needs.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public abstract class RemoteFileHandler {
    public static boolean handle(final String needId, final String content) {
        final ServerData server = Minecraft.getInstance().getCurrentServerData();
        if (server == null) return false;

        final String serverIP = server.serverIP;

        return true;
    }
}

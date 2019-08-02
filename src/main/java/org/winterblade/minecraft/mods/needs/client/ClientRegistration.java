package org.winterblade.minecraft.mods.needs.client;

import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.client.gui.widgets.NeedsUiButton;

public class ClientRegistration {

    private ClientRegistration() {}

    public static void register() {
        // Yep; I don't think I need to register a screen here since it's not a container?
        MinecraftForge.EVENT_BUS.addListener(NeedsUiButton::onUiOpened);
    }
}

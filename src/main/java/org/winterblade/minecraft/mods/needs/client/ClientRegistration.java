package org.winterblade.minecraft.mods.needs.client;

import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.client.gui.BarRenderDispatcher;
import org.winterblade.minecraft.mods.needs.client.gui.IconBarRenderer;
import org.winterblade.minecraft.mods.needs.client.gui.ProgressBarRenderer;
import org.winterblade.minecraft.mods.needs.client.gui.widgets.NeedsUiButton;
import org.winterblade.minecraft.mods.needs.mixins.HideBarMixin;
import org.winterblade.minecraft.mods.needs.mixins.IconBarMixin;
import org.winterblade.minecraft.mods.needs.mixins.ProgressBarMixin;

public class ClientRegistration {

    private ClientRegistration() {}

    public static void register() {
        // Yep; I don't think I need to register a screen here since it's not a container?
        MinecraftForge.EVENT_BUS.addListener(NeedsUiButton::onUiOpened);

        BarRenderDispatcher.registerBarRenderer(ProgressBarMixin.class, ProgressBarRenderer::getRenderer);
        BarRenderDispatcher.registerBarRenderer(IconBarMixin.class, IconBarRenderer::getRenderer);

        HideBarMixin.register();
    }
}

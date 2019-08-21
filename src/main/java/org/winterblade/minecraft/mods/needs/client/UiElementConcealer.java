package org.winterblade.minecraft.mods.needs.client;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UiElementConcealer {
    private final RenderGameOverlayEvent.ElementType type;

    public UiElementConcealer(final RenderGameOverlayEvent.ElementType type) {
        this.type = type;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    protected void onRender(final RenderGameOverlayEvent event) {
        if (event.getType() == type) event.setCanceled(true);
    }
}

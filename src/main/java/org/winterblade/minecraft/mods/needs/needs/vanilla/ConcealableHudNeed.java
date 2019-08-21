package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.CachedTickingNeed;
import org.winterblade.minecraft.mods.needs.api.needs.IHasHidableHudElement;
import org.winterblade.minecraft.mods.needs.client.UiElementConcealer;

@Document(description = "Includes some vanilla needs that can have their HUD element hidden")
public abstract class ConcealableHudNeed extends CachedTickingNeed implements IHasHidableHudElement {
    private UiElementConcealer concealer;
    private final RenderGameOverlayEvent.ElementType elementType;

    public ConcealableHudNeed(final RenderGameOverlayEvent.ElementType elementType) {
        this.elementType = elementType;
    }

    @Override
    public void loadConcealer() {
        concealer = new UiElementConcealer(elementType);
    }

    @Override
    public void unloadConcealer() {
        MinecraftForge.EVENT_BUS.unregister(concealer);
        concealer = null;
    }
}

package org.winterblade.minecraft.mods.needs.actions;

import com.google.gson.annotations.Expose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.LocalNeedLevelEvent;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.client.gui.OverlayRenderer;
import org.winterblade.minecraft.mods.needs.api.client.TextureResource;

@Document(description = "Renders an overlay on the player's UI while they're in this level")
public class OverlayAction extends LevelAction {
    @Expose
    @Document(description = "The overlay texture to draw.")
    private TextureResource overlay;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The width of the overlay; if 1 or lower, it's assumed to be a percentage, otherwise it's an absolute pixel value")
    @OptionalField(defaultValue = "Full Width")
    private double width = 1;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The height of the overlay; if 1 or lower, it's assumed to be a percentage, otherwise it's an absolute pixel value")
    @OptionalField(defaultValue = "Full Height")
    private double height = 1;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "For images that are not full width, the X offset to apply, in pixels; if negative, will be from the right.")
    @OptionalField(defaultValue = "0")
    private double x = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "For images that are not full height, the Y offset to apply, in pixels; if negative, will be from the bottom.")
    @OptionalField(defaultValue = "0")
    private double y = 0;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Whether or not this should count as a vignette or not; if true, the player will be able to " +
            "disable it by turning off fancy graphics. You should generally leave this enabled unless you have good reason " +
            "not to.")
    @OptionalField(defaultValue = "True")
    private boolean isVignette = true;

    private boolean display = false;

    @OnlyIn(Dist.CLIENT)
    private OverlayRenderer overlayRenderer;

    @Override
    public String getName() {
        return "Overlay";
    }

    @Override
    public void validate(final Need parentNeed, final NeedLevel parentLevel) throws IllegalArgumentException {
        super.validate(parentNeed, parentLevel);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            if (overlay == null) throw new IllegalArgumentException("Overlay action must have an overlay texture.");
        });
        if (width <= 0) throw new IllegalArgumentException("Width must be greater than 0.");
        if (height <= 0) throw new IllegalArgumentException("Height must be greater than 0.");
    }

    @Override
    public void onLoaded(final Need parentNeed, final NeedLevel parentLevel) {
        super.onLoaded(parentNeed, parentLevel);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            overlayRenderer = new OverlayRenderer(overlay, width, height, x, y);
        });

        parentNeed.enableSyncing();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    protected void onLocalLevelChanged(final LocalNeedLevelEvent event) {
        if (!matches(event.getNeed())) return;
        display = matches(event.getLevel());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    protected void onOverlayRender(final RenderGameOverlayEvent.Post event) {
        if (!display || overlayRenderer == null ||
                !(isVignette && event.getType() == RenderGameOverlayEvent.ElementType.VIGNETTE) &&
                !(!isVignette && event.getType() == RenderGameOverlayEvent.ElementType.BOSSINFO)) return;
        overlayRenderer.render();
    }
}

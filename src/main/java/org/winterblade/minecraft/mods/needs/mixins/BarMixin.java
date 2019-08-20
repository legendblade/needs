package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import org.winterblade.minecraft.mods.needs.api.Formatting;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.client.gui.BarRenderDispatcher;

@Document(description = "A collection of options for displaying bars on the user's HUD")
public abstract class BarMixin extends BaseMixin {
    @Expose
    @Document(description = "Optional icon to add to the left or right of the bar (depending on `iconOnRight`); not all bars may display that.")
    @OptionalField(defaultValue = "None")
    protected Icon icon;

    @Expose
    @Document(description = "Optional background to add behind the bar.")
    @OptionalField(defaultValue = "None")
    protected Icon background;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Where to position the bar relative to the screen; valid values are LEFT, CENTER, or RIGHT.")
    @OptionalField(defaultValue = "LEFT")
    private HorizontalAnchor horizontalAnchor = HorizontalAnchor.LEFT;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Where to position the bar relative to the screen; valid values are TOP, CENTER, or BOTTOM.")
    @OptionalField(defaultValue = "TOP")
    private VerticalAnchor verticalAnchor = VerticalAnchor.TOP;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Specifies if the optional icon from `icon` is on the left (false) or right (true) of the bar.")
    @OptionalField(defaultValue = "False")
    private boolean iconOnRight = false;

    @Expose
    @Document(description = "The X offset from the `horizontalAnchor` point; note this is where the left of the bar " +
            "is positioned")
    @OptionalField(defaultValue = "0")
    private int x;

    @Expose
    @Document(description = "The Y offset from the `verticalAnchor` point; note this is where the top of the bar " +
            "is positioned")
    @OptionalField(defaultValue = "0")
    private int y;

    @Expose
    @Document(description = "Defines any extra formatting parameters to apply")
    @OptionalField(defaultValue = "None")
    private Formatting formatting;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (icon != null) icon.validate();
        if (background != null) background.validate();
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        if (icon != null) icon.onLoaded();
        if (background != null) background.onLoaded();

        if (formatting == null) formatting = new Formatting();
        formatting.init();

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(BarRenderDispatcher.class));
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.unregister(BarRenderDispatcher.class));
    }

    public Icon getIcon() {
        return icon;
    }

    public HorizontalAnchor getHorizontalAnchor() {
        return horizontalAnchor;
    }

    public VerticalAnchor getVerticalAnchor() {
        return verticalAnchor;
    }

    public boolean isIconOnRight() {
        return iconOnRight;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Formatting getDisplayFormat() {
        return formatting;
    }

    public Icon getBackground() {
        return background;
    }

    public enum HorizontalAnchor {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum VerticalAnchor {
        TOP,
        CENTER,
        BOTTOM
    }
}

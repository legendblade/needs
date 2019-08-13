package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.ColorAdapter;

@Document(description = "Adds bars to the HUD")
public class BarMixin extends BaseMixin {
    @Expose
    @Document(description = "Color of the bar; optional if specifying `filledIcon`.")
    @OptionalField(defaultValue = "None")
    @JsonAdapter(ColorAdapter.class)
    private int color;

    @Expose
    @Document(description = "Icon to represent a filled portion of the bar; optional if specifying `color`.")
    @OptionalField(defaultValue = "None")
    private Icon filledIcon;

    @Expose
    @Document(description = "Icon to represent the background of the bar; only used if specifying `filledIcon`.")
    @OptionalField(defaultValue = "None")
    private Icon emptyIcon;

    @Expose
    @Document(description = "If using `filledIcon`, how much should one full icon represent.")
    @OptionalField(defaultValue = "0")
    private double iconValue;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "What element to anchor the bar to; valid values are: SCREEN, HEALTH, BREATH, ARMOR, or FOOD; " +
            "the latter four represent the respective bars.")
    @OptionalField(defaultValue = "SCREEN")
    private ScreenItem anchorTo = ScreenItem.SCREEN;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Where to position the bar relative to the anchor point; valid values are LEFT, CENTER, or RIGHT.")
    @OptionalField(defaultValue = "LEFT")
    private AnchorPoint horizontalAnchor = AnchorPoint.LEFT;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Where to position the bar relative to the anchor point; valid values are TOP, CENTER, or BOTTOM.")
    @OptionalField(defaultValue = "TOP")
    private AnchorPoint verticalAnchor = AnchorPoint.TOP;

    @Expose
    @Document(description = "Optional icon to add to the left or right of the bar (depending on `iconOnRight`).")
    @OptionalField(defaultValue = "None")
    private Icon icon;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Specifies if the optional icon from `icon` is on the left (false) or right (true) of the bar.")
    @OptionalField(defaultValue = "False")
    private boolean iconOnRight = false;

    @Expose
    @Document(description = "The width of the bar; optional if specifying `filledIcon`.")
    @OptionalField(defaultValue = "0")
    private int width;

    @Expose
    @Document(description = "The height of the bar; optional if specifying `filledIcon`")
    @OptionalField(defaultValue = "0")
    private int height;

    @Expose
    @Document(description = "The X offset from the `horizontalAnchor` point; note this is where the left of the bar " +
            "is positioned")
    @OptionalField(defaultValue = "0")
    private int offsetX;

    @Expose
    @Document(description = "The Y offset from the `verticalAnchor` point; note this is where the top of the bar " +
            "is positioned")
    @OptionalField(defaultValue = "0")
    private int offsetY;

    @Expose
    @Document(description = "An expression to return the value that should be used for display purposes; this is " +
            "used either for the text on the bar (if not using `filledIcon`) or to determine how many / what portion " +
            "of each icon to display (if using `filledIcon`).")
    private NeedExpressionContext displayFormat;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (filledIcon != null) {
            filledIcon.validate();
            if (emptyIcon != null) emptyIcon.validate();
            if (iconValue <= 0) throw new IllegalArgumentException("When using icons, icon value must be greater than zero.");
        } else {
            if (width <= 0) throw new IllegalArgumentException("When not using icons, width must be greater than zero.");
            if (height <= 0) throw new IllegalArgumentException("When not using icons, height must be greater than zero.");
        }

        if (icon != null) icon.validate();
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        if (filledIcon != null) filledIcon.onLoaded();
        if (emptyIcon != null) emptyIcon.onLoaded();
        if (icon != null) icon.onLoaded();
    }

    public enum ScreenItem {
        SCREEN,
        HEALTH,
        BREATH,
        ARMOR,
        FOOD
    }

    public enum AnchorPoint {
        TOP,
        LEFT,
        CENTER,
        BOTTOM,
        RIGHT
    }
}

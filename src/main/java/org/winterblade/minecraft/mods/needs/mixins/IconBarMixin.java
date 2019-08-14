package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Adds an icon-based bar similar to the food or breath meters to the user's HUD")
public class IconBarMixin extends BarMixin {
    @Expose
    @Document(description = "Icon to represent a filled portion of the bar.")
    private Icon filledIcon;

    @Expose
    @Document(description = "Icon to represent a negative portion of the bar (if any); if not specified, the minimum of the bar will be clamped at 0.")
    @OptionalField(defaultValue = "None")
    private Icon negativeIcon;

    @Expose
    @Document(description = "Icon to represent the background of the bar.")
    @OptionalField(defaultValue = "None")
    private Icon emptyIcon;

    @Expose
    @Document(description = "How much should one full icon represent.")
    private double iconValue;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The maximum number of icons to show.")
    @OptionalField(defaultValue = "10")
    private int maxIcons = 10;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The maximum number of icons to show on the negative side (if any).")
    @OptionalField(defaultValue = "0")
    private int maxNegativeIcons = 0;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        filledIcon.validate();

        if (emptyIcon != null) emptyIcon.validate();
        if (negativeIcon != null) negativeIcon.validate();
        if (iconValue <= 0) throw new IllegalArgumentException("Icon value must be greater than zero.");

        if (maxIcons < 0) throw new IllegalArgumentException("Max icons must be zero or more.");
        if (maxNegativeIcons < 0) throw new IllegalArgumentException("Max negative icons must be zero or more.");
        if (maxIcons + maxNegativeIcons < 1) throw new IllegalArgumentException("Max icons + max negative icons must be at least 1");
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        filledIcon.onLoaded();

        if (emptyIcon != null) emptyIcon.onLoaded();
        if (negativeIcon != null) negativeIcon.onLoaded();
    }

    public Icon getFilledIcon() {
        return filledIcon;
    }

    public Icon getNegativeIcon() {
        return negativeIcon;
    }

    public Icon getEmptyIcon() {
        return emptyIcon;
    }

    public double getIconValue() {
        return iconValue;
    }

    public double getMaxIcons() {
        return maxIcons;
    }

    public double getMaxNegativeIcons() {
        return maxNegativeIcons;
    }
}

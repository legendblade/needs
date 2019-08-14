package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Adds an icon-based bar similar to the food or breath meters to the user's HUD")
public class IconBarMixin extends BarMixin {
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

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        filledIcon.validate();

        if (emptyIcon != null) emptyIcon.validate();
        if (iconValue <= 0) throw new IllegalArgumentException("When using icons, icon value must be greater than zero.");
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        if (filledIcon != null) filledIcon.onLoaded();
        if (emptyIcon != null) emptyIcon.onLoaded();
    }
}

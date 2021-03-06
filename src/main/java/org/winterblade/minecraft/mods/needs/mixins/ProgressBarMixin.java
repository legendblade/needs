package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.ColorSet;

@Document(description = "Adds a colored progress bar to the user's HUD.")
public class ProgressBarMixin extends BarMixin {
    @Expose
    @Document(description = "Color of the bar.")
    @JsonAdapter(ColorSet.Deserializer.class)
    private ColorSet color;

    @Expose
    @Document(description = "The width of the bar.")
    private int width;

    @Expose
    @Document(description = "The height of the bar.")
    private int height;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "If the bar should show a text representation of its value")
    @OptionalField(defaultValue = "True")
    private boolean showText = true;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (width <= 0) throw new IllegalArgumentException("When not using icons, width must be greater than zero.");
        if (height <= 0) throw new IllegalArgumentException("When not using icons, height must be greater than zero.");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean showText() {
        return showText;
    }

    public int getColor() {
        return color.get();
    }
}

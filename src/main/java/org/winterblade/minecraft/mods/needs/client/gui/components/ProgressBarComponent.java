package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.fml.client.config.GuiUtils;

public class ProgressBarComponent extends BoundedComponent {
    private final boolean showText;
    private double min = 0;
    private double max = 1;
    private double value = 0;
    private int color = 0x00FFFF;
    private final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
    private int valueWidth;
    private String valueText;

    public ProgressBarComponent(final Rectangle2d bounds) {
        this(bounds, true);
    }

    public ProgressBarComponent(final Rectangle2d bounds, final boolean showText) {
        super(bounds);
        // TODO: Directions other than left-to-right
        this.showText = showText;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public void setMinMax(final double min, final double max) {
        if (min < max) {
            this.min = min;
            this.max = max;
        } else {
            this.min = max;
            this.max = min;
        }
    }

    public double getValue() {
        return value;
    }

    public void setValue(final double value) {
        this.value = value;
        if (showText) {
            valueText = Double.toString(value);
            valueWidth = fontRenderer.getStringWidth(valueText);
        }
    }

    public int getColor() {
        return color;
    }

    public void setColor(final int color) {
        this.color = 0xFF000000 + (color < 0
            ? 0
            : 0xFFFFFF < color
                ? 0xFFFFFF
                : color);
    }

    @Override
    public void draw(final int x, final int y) {
        final int right;
        if (value < min) {
            right = x;
        } else if (max < value || min == max) {
            right = x + bounds.getWidth();
        } else {
            right = x + (int) (Math.round((value - min) / (max - min) * bounds.getWidth()));
        }

        GuiUtils.drawGradientRect(0, x, y, right, y + bounds.getHeight(), color, color);

        if (!showText) return;
        fontRenderer.drawStringWithShadow(valueText, Math.max(right - valueWidth - 1, x + 1), y + 1, 0xFFFFFF);
    }
}

package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Rectangle2d;

@SuppressWarnings("WeakerAccess")
public class ProgressBarComponent extends ColorBarComponent {
    private final boolean showText;
    protected double min = 0;
    protected double max = 1;
    private double value = 0;
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

    public double getValue() {
        return value;
    }

    public void setValue(final double value, final String valueText) {
        this.value = value;
        if (showText) {
            this.valueText = valueText;
            valueWidth = fontRenderer.getStringWidth(valueText);
        }
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

        drawBar(y, x, right);

        if (!showText) return;
        fontRenderer.drawStringWithShadow(valueText, Math.max(right - valueWidth - 1, x + 1), y + 1, 0xFFFFFF);
    }
}

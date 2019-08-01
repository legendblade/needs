package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.fml.client.config.GuiUtils;

public class ProgressBarComponent extends BoundedComponent {
    private double min = 0;
    private double max = 1;
    private double value = 0;
    private int color = 0x00FFFF;

    public ProgressBarComponent(final Rectangle2d bounds) {
        super(bounds);
        // TODO: Directions other than left-to-right
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
            right = 0;
        } else if (max < value || min == max) {
            right = bounds.getWidth();
        } else {
            right = (int) (Math.round((value - min) / (max - min) * bounds.getWidth()));
        }

        GuiUtils.drawGradientRect(1, x, y, x + right, y + bounds.getHeight(), color, color);
    }
}

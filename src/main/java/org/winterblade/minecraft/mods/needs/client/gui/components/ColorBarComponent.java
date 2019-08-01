package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.fml.client.config.GuiUtils;

@SuppressWarnings({"WeakerAccess", "LocalCanBeFinal"})
public class ColorBarComponent extends BoundedComponent {
    protected int color = 0x00FFFF;
    protected int left = 0;
    protected int right = 0;

    public ColorBarComponent(final Rectangle2d bounds) {
        super(bounds);
    }

    public int getColor() {
        return color;
    }

    public void setColor(final int color) {
        this.color = color < 0
            ? color
            : 0xFFFFFF < color
                ? color
                : 0xFF000000 + color;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public void setRight(int right) {
        this.right = right;
    }

    @Override
    public void draw(final int x, final int y) {
        if (!isVisible()) return;
        drawBar(y, x + left, x + right);
    }

    protected void drawBar(final int y, final int left, final int right) {
        GuiUtils.drawGradientRect(0, left, y, right, y + bounds.getHeight(), getColor(), getColor());
    }
}

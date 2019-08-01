package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class MovableTextureComponent extends TexturedComponent {
    private int xOffset = 0;
    private int yOffset = 0;

    public MovableTextureComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);
    }

    public void setOffsets(final int xOffset, final int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    @Override
    public void draw(final int x, final int y) {
        if (!display) return;
        super.draw(x + xOffset, y + yOffset);
    }
}

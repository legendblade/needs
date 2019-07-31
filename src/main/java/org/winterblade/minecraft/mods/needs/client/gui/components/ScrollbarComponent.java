package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;

public class ScrollbarComponent extends BoundedComponent {
    private int currentOffset;

    public ScrollbarComponent(final Rectangle2d bounds) {
        super(bounds);
        currentOffset = 0;
    }

    @Override
    public void draw(final int x, final int y) {

    }

    public int getScrollOffset() {
        return currentOffset;
    }

    public void setScrollOffset(final int offset) {
        currentOffset = offset;
    }

    public void adjustScroll(final int amount) {
        currentOffset += amount;
    }
}

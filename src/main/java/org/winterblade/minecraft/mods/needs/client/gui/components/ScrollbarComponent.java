package org.winterblade.minecraft.mods.needs.client.gui.components;

public class ScrollbarComponent implements IComponent {
    private int currentOffset;

    public ScrollbarComponent() {
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
}

package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

import java.util.function.Supplier;

public class ScrollbarComponent extends BoundedComponent {
    private final Texture active;
    private final Texture inactive;
    private final Supplier<Integer> maxHeight;
    private final int activeHeight;
    private int currentOffset;
    private int max = 0;
    private boolean canScroll;

    public ScrollbarComponent(final Rectangle2d bounds, final Texture active, final Texture inactive, final Supplier<Integer> maxHeight) {
        super(bounds);
        this.active = active;
        this.inactive = inactive;
        this.maxHeight = maxHeight;
        this.activeHeight = bounds.getHeight() - active.getDrawHeight();
    }

    @Override
    public void draw(final int x, final int y) {
        max = maxHeight.get();
        if (max <= bounds.getHeight()) {
            canScroll = false;
            inactive.bind();
            inactive.draw(x, y, 1);
            return;
        }

        canScroll = true;
        final int handleY = Math.floorDiv(currentOffset, max) * activeHeight;
        active.bind();
        active.draw(x, y + handleY, 1);
    }

    public int getScrollOffset() {
        return currentOffset;
    }

    public void setScrollOffset(final int offset) {
        if (!canScroll) {
            currentOffset = 0;
            return;
        }
        currentOffset = Math.max(Math.min(offset, max - activeHeight), 0);
    }

    public void adjustScroll(final int amount) {
        setScrollOffset(currentOffset + amount);
    }
}

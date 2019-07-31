package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.screens.ComponentScreen;

import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class ScrollbarComponent extends BoundedComponent implements IMouseScrollListener, IMouseClickListener {
    private static final int SCROLL_SPEED = 15;

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
        final int prevMax = max;
        max = maxHeight.get();
        if (max <= 0) {
            currentOffset = 0;
            canScroll = false;
            inactive.bind();
            inactive.draw(x, y, 1);
            return;
        }
        if (max != prevMax) setScrollOffset(currentOffset);

        canScroll = true;
        final int handleY = (int) Math.floor((currentOffset / (double)max) * activeHeight);
        active.bind();
        active.draw(x, y + handleY, 1);
    }

    public int getScrollOffset() {
        return currentOffset;
    }

    public void setScrollOffset(final int offset) {
        currentOffset = canScroll
            ? Math.max(Math.min(offset, max), 0)
            : 0;
    }

    public void adjustScroll(final double amount) {
        setScrollOffset(currentOffset + (int) Math.ceil(amount * -SCROLL_SPEED));
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double lines) {
        adjustScroll(lines);
        return true;
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final ComponentScreen.MouseButtons button) {
        // TODO: This is lazy
        setScrollOffset((int) Math.floor((y/(double)activeHeight) * max));
        return true;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final ComponentScreen.MouseButtons button) {
        return true;
    }
}

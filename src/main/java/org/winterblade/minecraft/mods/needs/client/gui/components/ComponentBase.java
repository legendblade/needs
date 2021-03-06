package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.screens.ComponentScreen;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class ComponentBase extends BoundedComponent implements IMouseDragListener, IMouseScrollListener {
    private final List<IComponent> subcomponents = new ArrayList<>();
    private final List<IMouseClickListener> clickListeners = new ArrayList<>();
    private final List<IMouseDragListener> dragListeners = new ArrayList<>();
    private final List<IMouseScrollListener> scrollListeners = new ArrayList<>();

    public ComponentBase(final Rectangle2d bounds) {
        super(bounds);
    }

    /**
     * Adds a sub-component to this component at the given x/y offsets
     * @param component The component to add
     */
    public void addComponent(final IComponent component) {
        subcomponents.add(component);
        if (component instanceof IMouseClickListener) clickListeners.add((IMouseClickListener) component);
        if (component instanceof IMouseDragListener) dragListeners.add((IMouseDragListener) component);
        if (component instanceof IMouseScrollListener) scrollListeners.add((IMouseScrollListener) component);
    }

    /**
     * Draw the component
     * @param x The x offset to draw at
     * @param y The y offset to draw at
     */
    @Override
    public void draw(final int x, final int y) {
        if (!isVisible()) return;
        drawSubComponents(x, y);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final ComponentScreen.MouseButtons button) {
        for (final IMouseClickListener subcomponent : clickListeners) {
            if (!subcomponent.isVisible()) continue;
            if (isInBounds(x, y, subcomponent)) {
                final Rectangle2d sb = subcomponent.getBounds();
                return subcomponent.mouseClicked(x - sb.getX(), y - sb.getY(), button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final ComponentScreen.MouseButtons button) {
        for (final IMouseClickListener subcomponent : clickListeners) {
            if (!subcomponent.isVisible()) continue;
            if (isInBounds(x, y, subcomponent)) {
                final Rectangle2d sb = subcomponent.getBounds();
                return subcomponent.mouseReleased(x - sb.getX(), y - sb.getY(), button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final ComponentScreen.MouseButtons button, final double dragX, final double dragY) {
        for (final IMouseDragListener subcomponent : dragListeners) {
            if (!subcomponent.isVisible()) continue;
            if (isInBounds(x, y, subcomponent)) {
                final Rectangle2d sb = subcomponent.getBounds();
                return subcomponent.mouseDragged(x - sb.getX(), y - sb.getY(), button, dragX - sb.getX(), dragY - sb.getY());
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double lines) {
        for (final IMouseScrollListener subcomponent : scrollListeners) {
            if (!subcomponent.isVisible()) continue;
            if (isInBounds(x, y, subcomponent)) {
                final Rectangle2d sb = subcomponent.getBounds();
                return subcomponent.mouseScrolled(x - sb.getX(), y - sb.getY(), lines);
            }
        }
        return false;
    }

    protected static boolean isInBounds(final double x, final double y, final IComponent subcomponent) {
        final int x1 = subcomponent.getBounds().getX();
        final int x2 = subcomponent.getBounds().getX() + subcomponent.getBounds().getWidth();
        final int y1 = subcomponent.getBounds().getY();
        final int y2 = subcomponent.getBounds().getY() + subcomponent.getBounds().getHeight();
        return (x1 <= x && x <= x2 && y1 <= y && y <= y2);
    }

    protected void drawSubComponents(final int x, final int y) {
        for (final IComponent s : subcomponents) {
            if (!s.isVisible()) continue;
            final Rectangle2d subBounds = s.getBounds();
            s.draw(x + subBounds.getX(), y + subBounds.getY());
        }
    }
}

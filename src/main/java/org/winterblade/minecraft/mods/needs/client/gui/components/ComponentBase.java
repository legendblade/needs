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
        drawSubComponents(x, y);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final ComponentScreen.MouseButtons button) {
        for (final IMouseClickListener subcomponent : clickListeners) {
            if (isInBounds(x, y, subcomponent)) return subcomponent.mouseClicked(x, y, button);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final ComponentScreen.MouseButtons button) {
        for (final IMouseClickListener subcomponent : clickListeners) {
            if (isInBounds(x, y, subcomponent)) return subcomponent.mouseReleased(x, y, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final ComponentScreen.MouseButtons button, final double dragX, final double dragY) {
        for (final IMouseDragListener subcomponent : dragListeners) {
            if (isInBounds(x, y, subcomponent) && isInBounds(dragX, dragY, subcomponent)) return subcomponent.mouseDragged(x, y, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double lines) {
        for (final IMouseScrollListener subcomponent : scrollListeners) {
            if (isInBounds(x, y, subcomponent)) return subcomponent.mouseScrolled(x, y, lines);
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
            final Rectangle2d subBounds = s.getBounds();
            s.draw(x + subBounds.getX(), y + subBounds.getY());
        }
    }
}

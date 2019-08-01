package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;

@SuppressWarnings("WeakerAccess")
public abstract class BoundedComponent implements IComponent {
    protected final Rectangle2d bounds;
    protected boolean isVisible = true;

    protected BoundedComponent(final Rectangle2d bounds) {
        this.bounds = bounds;
    }

    @Override
    public Rectangle2d getBounds() {
        return bounds;
    }

    @Override
    public void setVisible(final boolean visible) {
        this.isVisible = visible;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }
}

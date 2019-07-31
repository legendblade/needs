package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;

public abstract class BoundedComponent implements IComponent {
    protected final Rectangle2d bounds;

    protected BoundedComponent(Rectangle2d bounds) {
        this.bounds = bounds;
    }

    @Override
    public Rectangle2d getBounds() {
        return bounds;
    }
}

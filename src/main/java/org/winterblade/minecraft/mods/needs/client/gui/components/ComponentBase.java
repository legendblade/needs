package org.winterblade.minecraft.mods.needs.client.gui.components;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class ComponentBase implements IComponent {
    private final List<SubComponent> subcomponents = new ArrayList<>();

    /**
     * Adds a sub-component to this component at the given x/y offsets
     * @param component The component to add
     * @param x         The x-offset
     * @param y         The y-offset
     */
    public void addComponent(final IComponent component, final int x, final int y) {
        subcomponents.add(new SubComponent(component, x, y));
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


    protected void drawSubComponents(final int x, final int y) {
        for (final SubComponent s : subcomponents) {
            s.component.draw(x + s.x, y + s.y);
        }
    }

    private class SubComponent {
        int x;
        int y;
        IComponent component;

        SubComponent(final IComponent component, final int x, final int y) {
            this.component = component;
            this.x = x;
            this.y = y;
        }
    }
}

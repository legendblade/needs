package org.winterblade.minecraft.mods.needs.client.gui.components;

import org.winterblade.minecraft.mods.needs.client.gui.screens.ComponentScreen;

public interface IMouseClickListener extends IComponent {
    /**
     * Called when the mouse is clicked
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @return       True if this should count as a drag, false otherwise?
     */
    boolean mouseClicked(double x, double y, ComponentScreen.MouseButtons button);

    /**
     * Called when the mouse is released
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @return       True if this should count as a drag, false otherwise?
     */
    boolean mouseReleased(double x, double y, ComponentScreen.MouseButtons button);
}

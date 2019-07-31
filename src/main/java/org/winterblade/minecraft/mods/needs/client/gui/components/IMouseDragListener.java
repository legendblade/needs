package org.winterblade.minecraft.mods.needs.client.gui.components;

import org.winterblade.minecraft.mods.needs.client.gui.screens.ComponentScreen;

public interface IMouseDragListener extends IMouseClickListener, IComponent {
    /**
     * Called when the mouse is released
     * @param x      The x coordinate of the mouse
     * @param y      The y coordinate of the mouse
     * @param button The button used, 0: left click, 1: right click, 2: middle, 3: 'back', 4: 'forward'
     * @param dragX  The end (start?) x position of the drag
     * @param dragY  The end (start?) y position of the drag
     * @return       True if this should count as a drag, false otherwise?
     */
    boolean mouseDragged(double x, double y, ComponentScreen.MouseButtons button, double dragX, double dragY);
}

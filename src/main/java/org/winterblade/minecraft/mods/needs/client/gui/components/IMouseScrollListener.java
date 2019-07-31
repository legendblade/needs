package org.winterblade.minecraft.mods.needs.client.gui.components;

public interface IMouseScrollListener extends IComponent {
    /**
     * Called when mouse is scrolled
     * @param x     The x coordinate of the mouse at the time of scroll
     * @param y     The y coordinate of the mouse at the time of scroll
     * @param lines The number of lines to scroll; -1 is down, 1 is up
     * @return      If scrolling occurred?
     */
    boolean mouseScrolled(double x, double y, double lines);
}

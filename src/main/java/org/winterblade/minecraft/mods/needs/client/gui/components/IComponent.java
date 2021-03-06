package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;

public interface IComponent {
    void draw(int x, int y);

    Rectangle2d getBounds();

    void setVisible(boolean visible);

    boolean isVisible();
}

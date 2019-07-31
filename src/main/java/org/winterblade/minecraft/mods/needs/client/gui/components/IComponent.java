package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.screens.ComponentScreen;

public interface IComponent {
    void draw(int x, int y);

    Rectangle2d getBounds();

}

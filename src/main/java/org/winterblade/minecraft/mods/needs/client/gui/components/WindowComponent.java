package org.winterblade.minecraft.mods.needs.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class WindowComponent extends TexturedComponent {

    public WindowComponent(final Texture texture) {
        super(texture);
    }

    @Override
    public void draw(final int x, final int y) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        super.draw(x, y);
    }
}

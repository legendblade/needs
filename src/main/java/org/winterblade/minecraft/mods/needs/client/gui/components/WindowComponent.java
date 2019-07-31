package org.winterblade.minecraft.mods.needs.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class WindowComponent extends TexturedComponent {

    public WindowComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);
    }

    public WindowComponent(Texture texture, int width, int height) {
        this(texture, new Rectangle2d(0, 0, width, height));
    }

    @Override
    public void draw(final int x, final int y) {
        if (!display) return;
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        super.draw(x, y);
    }
}

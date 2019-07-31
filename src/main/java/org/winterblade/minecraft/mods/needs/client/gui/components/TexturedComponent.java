package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class TexturedComponent extends ComponentBase {
    protected final Texture texture;

    public TexturedComponent(final Texture texture, final Rectangle2d bounds) {
        super(bounds);
        this.texture = texture;
    }

    @Override
    public void draw(final int x, final int y) {
        if (!display) return;
        texture.bind();
        texture.draw(x, y, 0);
        super.draw(x, y);
    }
}

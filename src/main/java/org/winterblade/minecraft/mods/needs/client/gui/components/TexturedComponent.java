package org.winterblade.minecraft.mods.needs.client.gui.components;

import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class TexturedComponent extends ComponentBase {
    protected final Texture texture;

    public TexturedComponent(final Texture texture) {
        this.texture = texture;
    }

    @Override
    public void draw(final int x, final int y) {
        texture.bind();
        texture.draw(x, y, 0);
        super.draw(x, y);
    }
}

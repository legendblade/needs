package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class NeedComponent extends TexturedComponent {
    private final TextComponent title;

    // TODO: lots more
    public NeedComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);
        title = new TextComponent("", 38, 3, 0xFFFFFF);
        addComponent(title);
        display = false;
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }
}

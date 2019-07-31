package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;

public class TextComponent extends ComponentBase {
    private final String text;
    private final int color;
    private final boolean hasShadow;

    public TextComponent(String text, int color) {
        this(text, color, false);
    }

    public TextComponent(String text, int color, boolean hasShadow) {
        this.text = text;
        this.color = color;
        this.hasShadow = hasShadow;
    }

    @Override
    public void draw(int x, int y) {
        Minecraft.getInstance().fontRenderer.drawStringWithShadow(text, x, y, color);
    }
}

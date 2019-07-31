package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Rectangle2d;

public class TextComponent implements IComponent {
    private final String text;
    private final int color;
    private final boolean hasShadow;
    private final FontRenderer fontRenderer;
    private final Rectangle2d bounds;

    public TextComponent(final String text, final int x, final int y, final int color) {
        this(text, x, y, color, false);
    }

    public TextComponent(final String text, final int x, final int y, final int color, final boolean hasShadow) {
        this.text = text;
        this.color = color;
        this.hasShadow = hasShadow;
        fontRenderer = Minecraft.getInstance().fontRenderer;
        this.bounds = new Rectangle2d(x, y, fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT);
    }

    @Override
    public void draw(final int x, final int y) {
        if (hasShadow) fontRenderer.drawStringWithShadow(text, x, y, color);
        else fontRenderer.drawString(text, x, y, color);
    }

    @Override
    public Rectangle2d getBounds() {
        return bounds;
    }
}

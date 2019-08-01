package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.common.util.TextTable;

public class TextComponent implements IComponent {
    private final int color;
    private final boolean hasShadow;
    private final TextTable.Alignment alignment;
    private final FontRenderer fontRenderer;

    private String text;
    private Rectangle2d bounds;
    private boolean isVisible = true;

    public TextComponent(final String text, final int x, final int y, final int color) {
        this(text, x, y, color, false);
    }

    public TextComponent(final String text, final int x, final int y, final int color, final boolean hasShadow) {
        this(text, x, y, color, hasShadow, TextTable.Alignment.LEFT);
    }

    public TextComponent(final String text, final int x, final int y, final int color, final TextTable.Alignment alignment) {
        this(text, x, y, color, false, alignment);
    }

    public TextComponent(final String text, final int x, final int y, final int color, final boolean hasShadow, final TextTable.Alignment alignment) {
        this.text = text;
        this.color = color;
        this.hasShadow = hasShadow;
        this.alignment = alignment;
        fontRenderer = Minecraft.getInstance().fontRenderer;
        this.bounds = new Rectangle2d(x, y, fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT);
    }

    @Override
    public void draw(final int x, final int y) {
        if (!isVisible) return;
        final int x1;

        switch (alignment) {
            default:
            case LEFT:
                x1 = x;
                break;
            case CENTER:
                x1 = x - (bounds.getWidth()/2);
                break;
            case RIGHT:
                x1 = x - bounds.getWidth();
                break;
        }

        if (hasShadow) fontRenderer.drawStringWithShadow(text, x1, y, color);
        else fontRenderer.drawString(text, x1, y, color);
    }

    @Override
    public Rectangle2d getBounds() {
        return bounds;
    }

    /**
     * Sets the text and updates the bounding box
     * @param text The new text
     */
    public void setText(String text) {
        this.text = text;
        this.bounds = new Rectangle2d(bounds.getX(), bounds.getY(), fontRenderer.getStringWidth(text), fontRenderer.FONT_HEIGHT);
    }

    public void setVisible(final boolean isVisible) {
        this.isVisible = isVisible;
    }
}

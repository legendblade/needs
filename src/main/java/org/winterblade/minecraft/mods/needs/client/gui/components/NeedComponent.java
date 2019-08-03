package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.common.util.TextTable;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class NeedComponent extends TexturedComponent {
    private static final int BAR_WIDTH = 242;

    private final TextComponent title;
    private final ProgressBarComponent progress;
    private final TextComponent minText;
    private final TextComponent maxText;
    private final TextComponent level;
    private final ColorBarComponent currentBounds;
    private final MovableTextureComponent lowerBound;
    private final MovableTextureComponent upperBound;
    private Texture icon;
    private int iconOffsetX;
    private int iconOffsetY;

    public NeedComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);

        title = new TextComponent("", 38, 4, 0xFFFFFF, true);
        addComponent(title);

        final Rectangle2d barBounds = new Rectangle2d(38, 15, BAR_WIDTH, 10);
        progress = new ProgressBarComponent(barBounds);
        addComponent(progress);

        minText = new TextComponent("", 37, 28, 0xFFFFFF);
        addComponent(minText);

        maxText = new TextComponent("", 281, 28, 0xFFFFFF, TextTable.Alignment.RIGHT);
        addComponent(maxText);

        level = new TextComponent("", 281, 4, 0xFFFFFF, true, TextTable.Alignment.RIGHT);
        addComponent(level);

        currentBounds = new ColorBarComponent(new Rectangle2d(38, barBounds.getY() + 2, BAR_WIDTH, barBounds.getHeight() - 4));
        currentBounds.setColor(0x77333333); // Transparent dark gray
        addComponent(currentBounds);

        final Texture boundsTexture = texture.getSubtexture(1, 14, 284, 222);
        lowerBound = new MovableTextureComponent(boundsTexture, new Rectangle2d(38, 13, 1, 14));
        upperBound = new MovableTextureComponent(boundsTexture, new Rectangle2d(38, 13, 1, 14));
        addComponent(lowerBound);
        addComponent(upperBound);

        setVisible(false);
    }

    @Override
    public void draw(final int x, final int y) {
        super.draw(x, y);

        // We're just cheating, because the icon can move around and change and (etc, etc) and I'm lazy:
        icon.bind();
        icon.draw(x + iconOffsetX + 3, y + iconOffsetY + 3,1);
    }

    public void setTitle(final String title) {
        this.title.setText(title);
    }

    /**
     * Sets the values for the progress bar, as well as the lower and upper bounds of the current level
     * @param min   The min value
     * @param max   The max value
     * @param value The current value
     * @param color The color of the bar
     * @param lower The lower bound
     * @param upper The upper bound
     */
    public void setBarValues(final double min, final double max, final double value, final int color, final double lower, final double upper) {
        progress.setMinMax(min, max);
        progress.setValue(value);
        progress.setColor(color);

        minText.setText(Double.toString(min));
        maxText.setText(Double.toString(max));

        if ((lower < min) && (max < upper)) {
            currentBounds.setVisible(false);
            lowerBound.setVisible(false);
            return;
        }

        currentBounds.setVisible(true);
        final double range = max - min;
        if (lower <= min) {
            lowerBound.setVisible(false);
            currentBounds.setLeft(0);
        } else {
            lowerBound.setVisible(true);
            lowerBound.setOffsets((int) (((lower - min) / range) * BAR_WIDTH), 0);
            currentBounds.setLeft((int) (((lower - min) / range) * BAR_WIDTH));
        }

        if (max <= upper) {
            upperBound.setVisible(false);
            currentBounds.setRight(BAR_WIDTH);
        } else {
            upperBound.setVisible(true);
            upperBound.setOffsets((int) (((upper - min) / range) * BAR_WIDTH), 0);
            currentBounds.setRight((int) (((upper - min) / range) * BAR_WIDTH));
        }
    }

    /**
     * Sets the name of the level
     * @param level         The name of the current level
     * @param shouldDisplay If we should display the level or not
     */
    public void setLevel(final String level, final boolean shouldDisplay) {
        if (!shouldDisplay) {
            this.level.setVisible(false);
            return;
        }

        this.level.setVisible(true);
        this.level.setText(level);
    }

    /**
     * Sets the icon to draw later
     * @param icon    The icon texture
     * @param offsetX The icon's X offset
     * @param offsetY The icon's Y offset
     */
    public void setIcon(final Texture icon, final int offsetX, final int offsetY) {
        this.icon = icon;
        this.iconOffsetX = offsetX;
        this.iconOffsetY = offsetY;
    }
}

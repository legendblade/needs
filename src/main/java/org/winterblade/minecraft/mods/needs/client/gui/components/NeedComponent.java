package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class NeedComponent extends TexturedComponent {
    private final TextComponent title;
    private final ProgressBarComponent progress;

    // TODO: lots more
    public NeedComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);

        title = new TextComponent("", 38, 3, 0xFFFFFF);
        addComponent(title);

        progress = new ProgressBarComponent(new Rectangle2d(39, 19, 241, 10));
        addComponent(progress);

        display = false;
    }

    public void setTitle(final String title) {
        this.title.setText(title);
    }

    /**
     * Sets the values for the progress bar
     * @param min   The min value
     * @param max   The max value
     * @param value The current value
     * @param color The color of the bar
     */
    public void setBarValues(final double min, final double max, final double value, final int color) {
        progress.setMinMax(min, max);
        progress.setValue(value);
        progress.setColor(color);
    }
}

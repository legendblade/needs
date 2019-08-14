package org.winterblade.minecraft.mods.needs.client.gui;

import net.minecraft.client.Minecraft;
import org.winterblade.minecraft.mods.needs.api.client.gui.IBarRenderer;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.mixins.BarMixin;
import org.winterblade.minecraft.mods.needs.mixins.IconBarMixin;

public class IconBarRenderer implements IBarRenderer {
    private final double iconValue;
    private final double max;
    private final double min;
    private final boolean showEmpty;
    private final double totalIcons;
    private final Icon emptyIcon;
    private final Icon negativeIcon;
    private final Icon fullIcon;
    private final int nx;

    private IconBarRenderer(final IconBarMixin mixin) {
        iconValue = mixin.getIconValue();

        // There are edge cases here where the max icons * value is greater than a double. I'm not sure I care?
        max = mixin.getMaxIcons() == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : mixin.getMaxIcons() * iconValue;
        min = mixin.getMaxNegativeIcons() <= 0 || mixin.getNegativeIcon() == null ? 0 : 0 - (mixin.getMaxNegativeIcons() * iconValue);
        totalIcons = mixin.getMaxIcons() + mixin.getMaxNegativeIcons();

        showEmpty = mixin.getEmptyIcon() != null;
        fullIcon = mixin.getFilledIcon();
        emptyIcon = mixin.getEmptyIcon();
        negativeIcon = mixin.getNegativeIcon();
        nx = negativeIcon != null ? negativeIcon.getWidth() : 0;
    }

    public static IBarRenderer getRenderer(@SuppressWarnings("unused") final Need need, final BarMixin mx) {
        return new IconBarRenderer((IconBarMixin) mx);
    }

    @Override
    public void render(final LocalCachedNeed need, final BarMixin mixin, final int x, final int y) {
        // Set up our actual bounds now that we have the need:
        final double min = this.min < 0 ? Math.max(this.min, need.getMin()) : 0;
        final double max = Math.min(this.max, need.getMax());

        // Get our value:
        final NeedExpressionContext fmt = mixin.getDisplayFormat();
        double value;
        if (fmt != null) {
            fmt.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, need::getValue);
            value = fmt.apply(Minecraft.getInstance().player);
        } else {
            value = need.getValue();
        }

        // Clamp it:
        if (value < min) value = min;
        else if (max < value) value = max;

        // Draw the background:
        if (showEmpty) {
            final ExpressionPositionedTexture texture = emptyIcon.getTexture();
            texture.setAndRecalculate(NeedExpressionContext.CURRENT_NEED_VALUE, need::getValue);
            texture.bind();

            for (int i = 0; i < totalIcons; i++) {
                texture.draw(
                        (x + (i * emptyIcon.getWidth())) + emptyIcon.getX(),
                        y + emptyIcon.getY(),
                        -50);
            }
        }

        // Find the point at which to start drawing from:
        final int center = min < 0
                ? (int) (((-min) / iconValue) * nx)
                : 0;

        // Setup our variables:
        final Icon icon;
        final int xi;
        final double dir;
        double leftToDraw;
        int i = 0;

        if (value < 0) {
            icon = negativeIcon != null ? negativeIcon : fullIcon;
            xi = -icon.getWidth();
            leftToDraw = -1 * value;
            i++;
            dir = -1;
        } else {
            icon = fullIcon;
            xi = icon.getWidth();
            leftToDraw = value;
            dir = 1;
        }

        // Do the render thing
        final ExpressionPositionedTexture texture = icon.getTexture();
        texture.setAndRecalculate(NeedExpressionContext.CURRENT_NEED_VALUE, need::getValue);
        texture.bind();

        while (0 < leftToDraw) {
            texture.draw(
                    (x + (i++ * xi)) + icon.getX() + center,
                    y + icon.getY(),
                    -49,
                    leftToDraw / iconValue * dir,
                    1.0);

            leftToDraw -= iconValue;
        }
    }
}

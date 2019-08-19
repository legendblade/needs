package org.winterblade.minecraft.mods.needs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rectangle2d;
import org.winterblade.minecraft.mods.needs.api.client.gui.IBarRenderer;
import org.winterblade.minecraft.mods.needs.api.client.gui.Icon;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.client.gui.components.ColorBarComponent;
import org.winterblade.minecraft.mods.needs.client.gui.components.ProgressBarComponent;
import org.winterblade.minecraft.mods.needs.mixins.BarMixin;
import org.winterblade.minecraft.mods.needs.mixins.ProgressBarMixin;

public class ProgressBarRenderer implements IBarRenderer {
    private final ColorBarComponent background;
    private final ProgressBarComponent component;

    private ProgressBarRenderer(final ProgressBarMixin mx) {
        final Rectangle2d bounds = new Rectangle2d(0, 0, mx.getWidth(), mx.getHeight());
        component = new ProgressBarComponent(bounds, mx.showText());
        final int color = mx.getColor();
        component.setColor(color);

        background = new ColorBarComponent(bounds);
        background.setColor(0x77000000 + (color & 0x888888));
        background.setRight(bounds.getWidth());
    }

    public static IBarRenderer getRenderer(@SuppressWarnings("unused") final Need need, final BarMixin mx) {
        return new ProgressBarRenderer((ProgressBarMixin) mx);
    }

    @Override
    public void render(final LocalCachedNeed need, final BarMixin mx, final int x, final int y) {
        // Set up everything...
        if (!(mx instanceof ProgressBarMixin)) return;
        final ProgressBarMixin mixin = (ProgressBarMixin) mx;
        component.setMinMax(need.getMin(), need.getMax());

        // Calculate our format
        final String display;
        final NeedExpressionContext fmt = mixin.getDisplayFormat();
        if (fmt != null) {
            fmt.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, need::getValue);
            display = fmt.apply(Minecraft.getInstance().player).toString();
        } else {
            display = String.format("%.1f", need.getValue());
        }

        component.setValue(need.getValue(), display);

        // Happy little accidents
        background.draw(x, y);
        component.draw(x, y);

        // Now bind and draw the icon texture:
        final Icon icon = mixin.getIcon();
        if (icon == null) return;

        final ExpressionPositionedTexture texture = icon.getTexture();
        texture.setAndRecalculate(NeedExpressionContext.CURRENT_NEED_VALUE, need::getValue);
        texture.bind();
        texture.draw(
            mx.isIconOnRight()
                ? x + icon.getX() + mixin.getWidth()
                : x + icon.getX() - icon.getWidth(),
            y + icon.getY(),
            1);
    }
}

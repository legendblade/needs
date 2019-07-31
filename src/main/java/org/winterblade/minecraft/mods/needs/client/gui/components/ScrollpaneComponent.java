package org.winterblade.minecraft.mods.needs.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rectangle2d;
import org.lwjgl.opengl.GL11;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ScrollpaneComponent extends BoundedComponent implements IMouseScrollListener {
    private final ScrollbarComponent bar;
    private final int maxItems;
    private final int componentHeight;
    private final BiConsumer<IComponent, Integer> populator;
    private final Minecraft client;

    private final IComponent[] components;

    /**
     *
     * @param bar             The scrollbar component
     * @param bounds          The bounds of the pane
     * @param itemCtor        Returns a component for the given item offset
     * @param componentHeight The height of individual components
     * @param populator       Function that accepts: the component and the offset
     */
    public ScrollpaneComponent(final ScrollbarComponent bar, final Rectangle2d bounds, final Function<Integer, IComponent> itemCtor,
                               final int componentHeight, final BiConsumer<IComponent, Integer> populator) {
        super(bounds);
        this.bar = bar;
        this.componentHeight = componentHeight;
        this.populator = populator;
        this.maxItems = Math.floorDiv(bounds.getHeight(), componentHeight)+2;

        // Create our list items
        components = new IComponent[maxItems];
        for (int i = 0; i < maxItems; i++) {
            components[i] = itemCtor.apply(i);
        }

        client = Minecraft.getInstance();
    }

    @Override
    public void draw(final int x, final int y) {
        final int scrollOffset = bar.getScrollOffset();
        final int offset = Math.floorDiv(scrollOffset, componentHeight);
        final int yOff = Math.floorMod(scrollOffset, componentHeight);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        final double scaleW = client.mainWindow.getWidth() / (double)client.mainWindow.getScaledWidth();
        final double scaleH = client.mainWindow.getHeight() / (double)client.mainWindow.getScaledHeight();

        final int x1 = (int) ((x) * scaleW);
        final int y1 = client.mainWindow.getHeight() - ((int) (y * scaleH) + (int) (bounds.getHeight() * scaleH));
        final int x2 = (int) (bounds.getWidth() * scaleW);
        final int y2 = (int) (bounds.getHeight() * scaleH);

        GL11.glScissor(x1, y1, x2, y2);

        for (int i = 0; i < maxItems; i++) {
            populator.accept(components[i], offset + i);
            components[i].draw(x, y - yOff + i * componentHeight);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double lines) {
        bar.adjustScroll((int) Math.ceil(lines * -5));
        return true;
    }
}

package org.winterblade.minecraft.mods.needs.api.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.ExpressionPositionedTexture;
import org.winterblade.minecraft.mods.needs.mixins.BarMixin;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiFunction;

public class BarRenderDispatcher {
    private static final IBarRenderer NOOP = new NoopRenderer();
    private static final Map<Class<? extends BarMixin>, BiFunction<Need, BarMixin, IBarRenderer>> renderRegistry = new HashMap<>();
    private static final WeakHashMap<BarMixin, IBarRenderer> renderers = new WeakHashMap<>();
    private static List<LocalCachedNeed> localCache;

    /**
     * Adds a factory function that will be called to get a renderer to render the given bar type. The factory will be
     * called once per instance of the given mixin type and the value will be kept around as long as that instance
     * exists.
     *
     * @param mixin    The mixin type
     * @param renderer The factory to get the renderer
     */
    public static void registerBarRenderer(final Class<? extends BarMixin> mixin, final BiFunction<Need, BarMixin, IBarRenderer> renderer) {
        renderRegistry.put(mixin, renderer);
    }


    @SuppressWarnings("unused")
    @SubscribeEvent
    protected static void onCacheUpdated(final LocalCacheUpdatedEvent event) {
        // Invalidate local cache
        localCache = null;
    }

    @SubscribeEvent
    protected static void onRender(final RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        final MainWindow win = Minecraft.getInstance().mainWindow;
        for (final LocalCachedNeed n : getLocalNeeds()) {
            final Need need = n.getNeed().get();
            if (need == null) continue;

            need
                .getMixins()
                .stream()
                .filter((m) -> m instanceof BarMixin)
                .forEach((b) -> {
                    final BarMixin theBar = (BarMixin) b;
                    // is too dang low

                    // I hate how switches look, but it's marginally faster and this is a render function.
                    final int x;
                    switch (theBar.getHorizontalAnchor()) {
                        case CENTER:
                            x = win.getScaledWidth() / 2;
                            break;
                        case RIGHT:
                            x = win.getScaledWidth();
                            break;
                        default:
                            x = 0;
                            break;
                    }

                    final int y;
                    switch (theBar.getVerticalAnchor()) {
                        case CENTER:
                            y = win.getScaledHeight() / 2;
                            break;
                        case BOTTOM:
                            y = win.getScaledHeight();
                            break;
                        default:
                            y = 0;
                            break;
                    }

                    // This works if here, and doesn't work if we move it to _outside_ the loop, and I have
                    // no idea why? Is it because we're using separate z-levels for drawing textures?
                    GlStateManager.disableDepthTest();
                    GlStateManager.depthMask(false);

                    final Icon bgIcon = theBar.getBackground();
                    if (bgIcon != null) {
                        final ExpressionPositionedTexture bg = bgIcon.getTexture();
                        bg.setAndRecalculate(NeedExpressionContext.CURRENT_NEED_VALUE, n::getValue);
                        bg.bind();
                        bg.draw(x + theBar.getX() + bgIcon.getX(), y + theBar.getY() + bgIcon.getY(), -90);
                    }

                    renderers
                            .computeIfAbsent(theBar,
                                    (bi) -> renderRegistry.getOrDefault(bi.getClass(), (n2, b2) -> NOOP).apply(need, bi))
                            .render(n, theBar,x + theBar.getX(), y + theBar.getY());

                    GlStateManager.depthMask(true);
                    GlStateManager.enableDepthTest();
                });
        }
    }

    /**
     * Called on the client side to get the local cache
     * @return  The cache
     */
    @Nonnull
    private static List<LocalCachedNeed> getLocalNeeds() {
        if (localCache != null) return localCache;

        // Iterate the map, pulling out any now invalid needs
        localCache = new ArrayList<>();

        // This will happen if a config update pulls out a need (which isn't possible yet, but Soon TM)
        NeedRegistry.INSTANCE.getLocalCache().forEach((key, value) -> {
            final Need need = value.getNeed().get();
            if (need == null || need.getMixins().stream().noneMatch((m) -> m instanceof BarMixin)) return;

            localCache.add(value);
        });

        // Finally, sort it by name
        return localCache;
    }

    private static class NoopRenderer implements IBarRenderer {
        @Override
        public void render(final LocalCachedNeed need, final BarMixin mx, final int x, final int y) {

        }
    }
}

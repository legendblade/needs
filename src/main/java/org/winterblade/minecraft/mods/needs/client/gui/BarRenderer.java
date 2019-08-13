package org.winterblade.minecraft.mods.needs.client.gui;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.mixins.BarMixin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BarRenderer {
    private static List<LocalCachedNeed> localCache;

    /**
     * Called on the client side to get the local cache
     * @return  The cache
     */
    @Nonnull
    public static List<LocalCachedNeed> getLocalNeeds() {
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

    @SuppressWarnings("unused")
    @SubscribeEvent
    protected static void onCacheUpdated(final LocalCacheUpdatedEvent event) {
        // Invalidate local cache
        localCache = null;
    }

    @SubscribeEvent
    protected static void onRender(final RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        for (final LocalCachedNeed n : getLocalNeeds()) {
            final Need need = n.getNeed().get();
            if (need == null) continue;

            need
                .getMixins()
                .stream()
                .filter((m) -> m instanceof BarMixin)
                .forEach((b) -> {
                    // TODO: Everything
                });
        }
    }
}

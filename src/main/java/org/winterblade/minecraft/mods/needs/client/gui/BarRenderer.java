package org.winterblade.minecraft.mods.needs.client.gui;

import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BarRenderer {
    private static List<LocalCachedNeed> sortedLocalCache;

    /**
     * Called on the client side to get the local cache
     * @return  The cache
     */
    @Nonnull
    public static List<LocalCachedNeed> getLocalNeeds() {
        if (sortedLocalCache != null) return sortedLocalCache;

        // Iterate the map, pulling out any now invalid needs
        sortedLocalCache = new ArrayList<>();

        // This will happen if a config update pulls out a need (which isn't possible yet, but Soon TM)
        NeedRegistry.INSTANCE.getLocalCache().forEach((key, value) -> {
            final Need need = value.getNeed().get();
            if (need == null) return;

            sortedLocalCache.add(value);
        });

        // Finally, sort it by name
        sortedLocalCache.sort(Comparator.comparing(LocalCachedNeed::getName));
        return sortedLocalCache;
    }

    @SuppressWarnings("unused")
    private static void onCacheUpdated(final LocalCacheUpdatedEvent event) {
        // Invalidate local cache
        sortedLocalCache = null;
    }
}

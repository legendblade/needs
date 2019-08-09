package org.winterblade.minecraft.mods.needs.api.needs;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

import java.util.HashMap;
import java.util.Map;

@Document(description = "A collection of needs that are updated and cached periodically")
public abstract class CachedTickingNeed extends Need {
    private final Map<String, Double> cache = new HashMap<>();

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        cache.clear();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    private void onTick(final PlayerEntity p) {
        final double current = getValue(p);
        final double prev = cache.computeIfAbsent(p.getCachedUniqueIdString(), (p2) -> current);
        if (current == prev) return;

        sendUpdates(p, BaseManipulator.EXTERNAL, prev, current);
        cache.put(p.getCachedUniqueIdString(), current);
    }
}

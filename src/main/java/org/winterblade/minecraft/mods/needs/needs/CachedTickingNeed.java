package org.winterblade.minecraft.mods.needs.needs;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.HashMap;
import java.util.Map;

public abstract class CachedTickingNeed extends Need {
    private Map<String, Double> cache = new HashMap<>();

    @Override
    public void onCreated() {
        NeedRegistry.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
    }

    private void onTick(PlayerEntity p) {
        double current = getValue(p);
        double prev = cache.computeIfAbsent(p.getCachedUniqueIdString(), (p2) -> current);
        if (current == prev) return;

        MinecraftForge.EVENT_BUS.post(new NeedAdjustmentEvent.Post(this, p, BaseManipulator.EXTERNAL, current, prev));
        cache.put(p.getCachedUniqueIdString(), current);
    }
}

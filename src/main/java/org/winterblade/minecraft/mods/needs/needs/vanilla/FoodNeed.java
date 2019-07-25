package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

import java.util.HashMap;
import java.util.Map;

public class FoodNeed extends Need {
    private static Map<String, Integer> cache = new HashMap<>();
    private static FoodNeed DUMMY = new FoodNeed();

    static {
        MinecraftForge.EVENT_BUS.addListener(FoodNeed::onTick);
    }

    private static void onTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) return;

        // TODO: this needs to be extracted into a single class so we're not iterating world players everywhere.
        event.world
            .getPlayers()
            .forEach((p) -> {
                int current = p.getFoodStats().getFoodLevel();
                int prev = cache.computeIfAbsent(p.getCachedUniqueIdString(), (p2) -> current);
                if (current == prev) return;

                MinecraftForge.EVENT_BUS.post(new NeedAdjustmentEvent.Post(DUMMY, p, BaseManipulator.EXTERNAL, current, prev));
                cache.put(p.getCachedUniqueIdString(), current);
            });
    }

    @Override
    public void onCreated() {

    }

    @Override
    public String getName() {
        return "Food";
    }

    @Override
    public double getMin(PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(PlayerEntity player) {
        return 20;
    }

    @Override
    public void initialize(PlayerEntity player) {
        // Do nothing.
    }

    @Override
    public double getValue(PlayerEntity player) {
        return player.getFoodStats().getFoodLevel();
    }

    @Override
    public boolean isValueInitialized(PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(PlayerEntity player, double newValue) {
        player.getFoodStats().setFoodLevel((int) Math.round(newValue));
    }
}

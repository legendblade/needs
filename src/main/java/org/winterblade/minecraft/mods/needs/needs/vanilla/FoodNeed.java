package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.needs.CachedTickingNeed;

public class FoodNeed extends CachedTickingNeed {

    @Override
    public String getName() {
        return "Food";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return 20;
    }

    @Override
    public void initialize(final PlayerEntity player) {
        // Do nothing.
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.getFoodStats().getFoodLevel();
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(final PlayerEntity player, final double newValue) {
        player.getFoodStats().setFoodLevel((int) Math.round(newValue));
    }

}

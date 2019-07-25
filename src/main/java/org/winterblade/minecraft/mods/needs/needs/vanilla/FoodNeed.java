package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;

public class FoodNeed extends Need {
    @Override
    public void onCreated() {

    }

    @Override
    public String getName() {
        return "Food";
    }

    @Override
    public int getMin(PlayerEntity player) {
        return 0;
    }

    @Override
    public int getMax(PlayerEntity player) {
        return 20;
    }

    @Override
    public void initialize(PlayerEntity player) {
        // Do nothing.
    }

    @Override
    public int getValue(PlayerEntity player) {
        return player.getFoodStats().getFoodLevel();
    }

    @Override
    public boolean isValueInitialized(PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(PlayerEntity player, int newValue) {
        player.getFoodStats().setFoodLevel(newValue);
    }
}

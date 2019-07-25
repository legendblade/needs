package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;

public class HealthNeed extends Need {
    @Override
    public String getName() {
        return "Health";
    }

    @Override
    public double getMin(PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(PlayerEntity player) {
        return Math.round(player.getMaxHealth());
    }

    @Override
    public double getValue(PlayerEntity player) {
        return Math.round(player.getMaxHealth());
    }

    @Override
    public boolean isValueInitialized(PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(PlayerEntity player, double newValue) {
        player.setHealth((float) newValue);
    }
}

package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;

public class HealthNeed extends Need {
    @Override
    public String getName() {
        return "Health";
    }

    @Override
    public int getMin(PlayerEntity player) {
        return 0;
    }

    @Override
    public int getMax(PlayerEntity player) {
        return Math.round(player.getMaxHealth() * 2);
    }

    @Override
    public int getValue(PlayerEntity player) {
        return Math.round(player.getMaxHealth() * 2);
    }

    @Override
    public boolean isValueInitialized(PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(PlayerEntity player, int newValue) {
        player.setHealth(newValue / 2);
    }
}

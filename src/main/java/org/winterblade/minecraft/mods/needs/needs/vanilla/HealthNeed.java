package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.needs.CachedTickingNeed;

public class HealthNeed extends CachedTickingNeed {
    @Override
    public String getName() {
        return "Health";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return Math.round(player.getMaxHealth());
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return Math.round(player.getHealth());
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }

    @Override
    protected void setValue(final PlayerEntity player, final double newValue) {
        player.setHealth((float) newValue);
    }

}

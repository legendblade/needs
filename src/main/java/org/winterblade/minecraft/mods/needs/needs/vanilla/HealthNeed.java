package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.CachedTickingNeed;

@Document(description = "Tracks the player's current health")
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
    protected double setValue(final PlayerEntity player, final double newValue, final double adjustAmount) {
        player.setHealth((float) newValue);
        return getValue(player);
    }

}

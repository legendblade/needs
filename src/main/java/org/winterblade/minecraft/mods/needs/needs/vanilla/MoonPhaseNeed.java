package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

@Document(description = "Tracks the moon phase (0.0-1.0) of the current dimension the player is in")
public class MoonPhaseNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Moon Phase";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return 1;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.world.getCurrentMoonPhaseFactor();
    }
}

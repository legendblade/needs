package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

@Document(description = "Tracks the saturation level of the player")
public class SaturationNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Saturation";
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
    public double getValue(final PlayerEntity player) {
        return player.getFoodStats().getSaturationLevel();
    }
}

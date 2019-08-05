package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.winterblade.minecraft.mods.needs.api.ReadOnlyNeed;

public class SunlightNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Sunlight";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return 15;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.world.getLightFor(LightType.SKY, new BlockPos(player));
    }
}

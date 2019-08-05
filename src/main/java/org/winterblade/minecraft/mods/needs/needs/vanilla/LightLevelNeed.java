package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

public class LightLevelNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Light Level";
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
        return player.world.getLight(new BlockPos(player));
    }
}

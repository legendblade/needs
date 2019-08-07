package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

@Document(description = "Tracks the vanilla temperature based on biome and Y-level")
public class TemperatureNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Temperature";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return -1000; // Technically there isn't one?
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return 1000; // Technically there isn't one?
    }

    @Override
    public double getValue(final PlayerEntity player) {
        final BlockPos pos = new BlockPos(player);
        return player.world.getBiome(pos).getTemperature(pos);
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }
}

package org.winterblade.minecraft.mods.needs.needs.vanilla;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

public class DistanceFromSpawnNeed extends ReadOnlyNeed {
    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Should the distance be square rooted automatically; this is more computationally intensive " +
            "but will produce the actual distance (`a^2 + b^2 = c^2` after all).")
    private boolean squareRoot;

    public DistanceFromSpawnNeed() {
        squareRoot = true;
    }

    @Override
    public String getName() {
        return "Distance from Spawn";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        final BlockPos spawn = player.world.getSpawnPoint();
        final double x = player.posX - spawn.getX();
        final double y = player.posY - spawn.getY();
        final double c = x * x + y * y;
        return squareRoot ? Math.sqrt(c) : c;
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.BlockCheckingManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered while the player is looking at, every 5 ticks.")
public class LookingAtManipulator extends BlockCheckingManipulator {

    @Expose
    @OptionalField(defaultValue = "6")
    @Document(description = "The ray-traced distance to check for the block")
    protected int distance = 6;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (distance <= 0) throw new IllegalArgumentException("Distance must be a positive whole number.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
        super.onLoaded();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    private void onTick(final PlayerEntity player) {
        if (failsDimensionCheck(player)) return;
        // Get start and end points:
        final Vec3d startingPosition = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        final Vec3d endingPosition = startingPosition.add(player.getLookVec().scale(distance));

        final RayTraceContext ctx = new RayTraceContext(
                startingPosition,
                endingPosition,
                RayTraceContext.BlockMode.OUTLINE, // Going to assume outline is faster than collision
                RayTraceContext.FluidMode.NONE, // We're going to ignore fluids for now...
                player
        );

        // We'd probably have to trace ourselves to detect if the player is looking at an entity on the server side
        // See ProjectileHelper before going that route.

        final BlockRayTraceResult result = player.world.rayTraceBlocks(ctx);
        //noinspection ConstantConditions - no, you probably aren't.
        if (result == null || result.getType() == RayTraceResult.Type.MISS) return;

        final BlockState target = player.world.getBlockState(result.getPos());
        if (!isMatch(target)) return;

        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.get(), this);
    }

}

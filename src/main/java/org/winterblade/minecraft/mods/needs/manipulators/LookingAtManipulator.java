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
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered while the player is looking at, every 5 ticks.")
public class LookingAtManipulator extends BlockCheckingManipulator {
    @Expose
    @OptionalField(defaultValue = "6")
    @Document(description = "The ray-traced distance to check for the block")
    protected int distance = 6;

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::handleManipulator);
        super.onLoaded();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::handleTrigger);
        super.onTriggerLoaded(parentNeed, parentCondition);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerUnloaded() {
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player);
    }

    @Override
    public boolean test(final PlayerEntity player) {
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
        if (result == null || result.getType() == RayTraceResult.Type.MISS) return false;

        final BlockState target = player.world.getBlockState(result.getPos());
        return isMatch(target);
    }

    @Override
    protected void validateCommon() {
        if (distance <= 0) throw new IllegalArgumentException("Distance must be a positive whole number.");
        super.validateCommon();
    }

    private void handleManipulator(final PlayerEntity player) {
        if (!test(player)) return;
        parent.adjustValue(player, getAmount(player), this);
    }

    private void handleTrigger(final PlayerEntity player) {
        if (!test(player)) return;
        parentCondition.trigger(player, this);
    }
}

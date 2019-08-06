package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

public class LookingAtManipulator extends BlockCheckingManipulator<NeedExpressionContext> {

    @Expose
    protected int distance = 6;

    @Override
    public void onCreated() {
        if (blocks.size() <= 0) {
            throw new JsonParseException("On/near block manipulator must have at least one block predicate.");
        }

        TickManager.INSTANCE.requestPlayerTickUpdate(this::onTick);
        super.onCreated();
    }

    private void onTick(final PlayerEntity player) {
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

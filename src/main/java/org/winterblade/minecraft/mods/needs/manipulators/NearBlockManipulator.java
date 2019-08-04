package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.util.blocks.BlockStatePredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.IBlockPredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.TagBlockPredicate;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;

public class NearBlockManipulator extends BaseManipulator {
    @Expose
    protected NearBlockExpressionContext amount;

    @Expose
    protected List<IBlockPredicate> blocks = Collections.emptyList();

    @Expose
    protected NeedExpressionContext radius = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    private static final BlockState VOID_AIR = Blocks.VOID_AIR.getDefaultState();

    private Function<PlayerEntity, Double> onTickFn;
    private Function<BlockState, Boolean> matchFn;

    public void onCreated() {
        if (blocks.size() <= 0) {
            throw new JsonParseException("On/near block manipulator must have at least one block predicate.");
        }

        // Optimize the list to only predicates that will match:
        final IForgeRegistry<Block> blockReg = RegistryManager.ACTIVE.getRegistry(Block.class);
        final Iterator<IBlockPredicate> iter = blocks.iterator();

        /*
            TODO: There are probably extra optimizations that can be made here, including
            precompiling a TreeSet of states that match when comparing against larger lists of predicates
        */
        while (iter.hasNext()) {
            final IBlockPredicate predicate = iter.next();
            if (predicate instanceof TagBlockPredicate) continue; // Tags aren't registered yet
            boolean matches = false;

            for (final Block block : blockReg.getValues()) {
                if(predicate instanceof BlockStatePredicate) {
                    matches = block.getStateContainer().getValidStates().stream().anyMatch(predicate);
                } else {
                    matches = predicate.test(block.getDefaultState());
                }

                if (matches) break;
            }

            if (matches) continue;

            NeedsMod.LOGGER.info("Predicate will match no blocks; it will be removed");
            iter.remove();
        }

        if (blocks.size() <= 0) {
            throw new JsonParseException("On/near block manipulator has no predicates that will match blocks.");
        } else if (blocks.size() <= 1) {
            final IBlockPredicate head = blocks.get(0);
            matchFn = head::test;
        } else if (blocks.stream().anyMatch((b) -> b.test(VOID_AIR))) {
            // If we need to match void blocks, sort the list accordingly

            // Move any predicates that match void air up to the top
            // This will match out-of-bounds blocks faster
            blocks.sort((b, b2) -> {
                if (b.equals(b2)) return 0;

                final boolean va = b.test(VOID_AIR);
                return va == b2.test(VOID_AIR) ? b.compareTo(b2) : va ? -1 : 1;
            });

            // Use the matcher that will count void blocks:
            matchFn = this::isMatch;
        } else {
            // Sort the list normally, but use the non-void matcher:
            blocks.sort(Comparable::compareTo);
            matchFn = this::isNonVoidMatch;
        }

        // Predetermine the most expedient function to use:
        if (radius.isConstant()) {
            final long radius = Math.round(Math.abs(this.radius.get()));

            // Much precompile, very wow
            onTickFn = radius <= 0
                ? amount.isConstant()
                    ? this::constantAmountZeroRadius
                    : this::variableAmountZeroRadius
                : amount.isConstant()
                    ? (p) -> constantAmountConstantRadius(p, radius)
                    : (p) -> variableAmountConstantRadius(p, radius);
        } else {
            onTickFn = amount.isConstant()
                ? this::constantAmountVariableRadius
                : this::variableAmountVariableRadius;
        }

        /*
            TODO: further optimization can occur if the value is already at or outside of the bounds, and
            the amount would cause the need/manipulator to go further OOB.
            This would need to test amount fn to see if it's negative or positive - which could change
            at various points (cos, sin, n - count, etc)
        */

        NeedRegistry.INSTANCE.requestPlayerTickUpdate(this::onTick);
    }

    /**
     * Gets the block position at the player's feet
     * @param player The player
     * @return The {@link BlockPos}
     */
    private static BlockPos getBlockAtFeet(final PlayerEntity player) {
        return new BlockPos(player.posX, player.posY - 1, player.posZ);
    }

    /**
     * Called when ticking to calculate
     * @param player The player
     */
    private void onTick(@Nonnull final PlayerEntity player) {
        parent.adjustValue(player, onTickFn.apply(player), this);
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is constant and {@link NearBlockManipulator#radius}
     * is constant zero
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private double constantAmountZeroRadius(final PlayerEntity player) {
        final BlockPos center = getBlockAtFeet(player);
        final BlockState state = player.world.getBlockState(center);

        return matchFn.apply(state) ? amount.get() : 0;
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is variable and {@link NearBlockManipulator#radius}
     * is constant zero
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private double variableAmountZeroRadius(final PlayerEntity player) {
        final BlockPos center = getBlockAtFeet(player);
        final BlockState state = player.world.getBlockState(center);

        amount.setIfRequired("count", () -> matchFn.apply(state) ? 1d : 0);
        return amount.get();
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is constant and {@link NearBlockManipulator#radius}
     * is constant and more than zero
     * @param player The player
     * @param radius The radius
     * @return 1 if the block matches, 0 otherwise
     */
    private double constantAmountConstantRadius(final PlayerEntity player, final long radius) {
        return isWithin(player.world, radius, getBlockAtFeet(player)) ? amount.get() : 0;
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is variable and {@link NearBlockManipulator#radius}
     * is constant and more than zero
     * @param player The player
     * @param radius The radius
     * @return 1 if the block matches, 0 otherwise
     */
    private double variableAmountConstantRadius(final PlayerEntity player, final long radius) {
        amount.setIfRequired("count", () -> (double) getCountWithin(player.world, radius, getBlockAtFeet(player)));
        return amount.get();
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is constant and {@link NearBlockManipulator#radius}
     * is variable
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private double constantAmountVariableRadius(final PlayerEntity player) {
        radius.setCurrentNeedValue(parent, player);
        final Double radius = this.radius.get();
        return isWithin(player.world, radius, getBlockAtFeet(player)) ? amount.get() : 0;
    }

    /**
     * Called if both {@link NearBlockManipulator#amount} and {@link NearBlockManipulator#radius} are variable
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private double variableAmountVariableRadius(final PlayerEntity player) {
        radius.setCurrentNeedValue(parent, player);
        amount.setIfRequired("count", () -> (double) getCountWithin(player.world, radius.get(), getBlockAtFeet(player)));
        return amount.get();
    }

    /**
     * Checks if the {@link BlockState} matches our predicates
     * @param state The state
     * @return True if it does, false otherwise
     */
    private boolean isMatch(final BlockState state) {
        for (final IBlockPredicate predicate : blocks) {
            if (predicate.test(state)) return true;
        }
        return false;
    }

    /**
     * Checks if the {@link BlockState} matches our predicates
     * @param state The state
     * @return True if it does, false otherwise
     */
    private boolean isNonVoidMatch(final BlockState state) {
        if (state.equals(VOID_AIR)) return false;

        for (final IBlockPredicate predicate : blocks) {
            if (predicate.test(state)) return true;
        }
        return false;
    }

    /**
     * Checks if any blocks within the given radius match our predicate; returns on the first match
     * @param world  The {@link World} to check
     * @param radius The radius to check within
     * @param center The {@link BlockPos} at the center
     * @return True if at least one block matches, false otherwise
     */
    private boolean isWithin(final World world, final double radius, final BlockPos center) {
        for (double x = -radius; x <= radius; x += 1d) {
            for (double y = -radius; y <= radius; y += 1d) {
                for (double z = -radius; z <= radius; z += 1d) {
                    if (matchFn.apply(world.getBlockState(center.add(x, y, z)))) return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks how many blocks within the given radius match our predicate; iterates the entire radius
     * @param world  The {@link World} to check
     * @param radius The radius to check within
     * @param center The {@link BlockPos} at the center
     * @return The number of matching blocks
     */
    private int getCountWithin(final World world, final double radius, final BlockPos center) {
        int i = 0;
        for (double x = -radius; x <= radius; x += 1d) {
            for (double y = -radius; y <= radius; y += 1d) {
                for (double z = -radius; z <= radius; z += 1d) {
                    if (!matchFn.apply(world.getBlockState(center.add(x, y, z)))) continue;
                    i++;
                    break;
                }
            }
        }

        return i;
    }

    @JsonAdapter(ExpressionContext.Deserializer.class)
    public static class NearBlockExpressionContext extends NeedExpressionContext {
        public NearBlockExpressionContext() {
        }

        @Override
        protected List<String> getElements() {
            final List<String> elements = super.getElements();
            elements.add("count");
            return elements;
        }
    }
}

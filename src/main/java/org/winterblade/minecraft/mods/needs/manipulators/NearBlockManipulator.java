package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.CountedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BlockCheckingManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.blocks.BlockStatePredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.IBlockPredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.TagBlockPredicate;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.Function;

@Document(description = "Checks the immediate surroundings of the player for the given block(s); will fire every " +
        "5 ticks.")
public class NearBlockManipulator extends BlockCheckingManipulator {
    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "The radius to check around the player; defaults to 0, which only targets the block " +
            "the player is standing ontop of.")
    protected NeedExpressionContext radius = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    private static final BlockState VOID_AIR = Blocks.VOID_AIR.getDefaultState();

    private Function<PlayerEntity, Integer> counter;
    private Function<BlockState, Boolean> matchFn;
    private double lastCount;

    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asManipulator);
        super.onLoaded();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asTrigger);
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
    public boolean test(final PlayerEntity player) {
        return 0 < counter.apply(player);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;

        amount.setIfRequired(CountedExpressionContext.COUNT, () -> lastCount);
        return amount.apply(player);
    }

    @Override
    protected void validateCommon() {
        if (radius.isConstant() && radius.apply(null) < 0) throw new IllegalArgumentException("Constant radius must be greater than zero.");
        super.validateCommon();
    }

    @Override
    protected void onLoadedCommon() {
        super.onLoadedCommon();
        checkBlockList();
        setMatchingFunction();

        // Predetermine the most expedient function to use:
        final String amountType = amount == null || amount.isConstant() || !amount.isRequired(CountedExpressionContext.COUNT) ? "" : ", Per";
        if (radius.isConstant()) {
            // This is only okay because we've checked that it's constant
            final long radius = Math.round(Math.abs(this.radius.apply(null)));

            // Much precompile, very wow
            if (radius <= 0) {
                counter = this::variableAmountZeroRadius;
                postFormat = (sb, player) -> sb.append("  (Standing On)").toString();
            } else {
                counter = amount == null || amount.isConstant()
                        ? (p) -> constantAmountConstantRadius(p, radius)
                        : (p) -> variableAmountConstantRadius(p, radius);
                final String range = "  (Within " + radius + " Block" + (radius == 1 ? "" : "s") + amountType + ")";
                postFormat = (sb, player) -> sb.append(range).toString();
            }
        } else {
            counter = amount == null || amount.isConstant()
                    ? this::constantAmountVariableRadius
                    : this::variableAmountVariableRadius;
            postFormat = (sb, player) -> {
                radius.setCurrentNeedValue(parent, player);
                final int radius = (int)Math.floor(this.radius.apply(player));
                return sb.append("  (Within ").append(this.radius).append(" Block").append(radius == 1 ? "" : "s").append(amountType).append(")").toString();
            };
        }

        /*
            TODO: further optimization can occur if the value is already at or outside of the bounds, and
            the amount would cause the need/manipulator to go further OOB.
            This would need to test amount fn to see if it's negative or positive - which could change
            at various points (cos, sin, n - count, etc)
        */
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
    private void asManipulator(@Nonnull final PlayerEntity player) {
        lastCount = counter.apply(player);
        if (lastCount <= 0) return;

        parent.adjustValue(player, getAmount(player), this);
    }

    /**
     * Called when ticking to calculate
     * @param player The player
     */
    private void asTrigger(@Nonnull final PlayerEntity player) {
        lastCount = counter.apply(player);
        if (lastCount <= 0) return;
        parentCondition.trigger(player, this);
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is variable and {@link NearBlockManipulator#radius}
     * is constant zero
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private int variableAmountZeroRadius(final PlayerEntity player) {
        final BlockPos center = getBlockAtFeet(player);
        final BlockState state = player.world.getBlockState(center);

        return matchFn.apply(state) ? 1 : 0;
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is constant and {@link NearBlockManipulator#radius}
     * is constant and more than zero
     * @param player The player
     * @param radius The radius
     * @return 1 if the block matches, 0 otherwise
     */
    private int constantAmountConstantRadius(final PlayerEntity player, final long radius) {
        return isWithin(player.world, radius, getBlockAtFeet(player));
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is variable and {@link NearBlockManipulator#radius}
     * is constant and more than zero
     * @param player The player
     * @param radius The radius
     * @return The count of blocks inside the given radius
     */
    private int variableAmountConstantRadius(final PlayerEntity player, final long radius) {
        return getCountWithin(player.world, radius, getBlockAtFeet(player));
    }

    /**
     * Called if {@link NearBlockManipulator#amount} is constant and {@link NearBlockManipulator#radius}
     * is variable
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private int constantAmountVariableRadius(final PlayerEntity player) {
        radius.setCurrentNeedValue(parent, player);
        final double radius = this.radius.apply(player);
        return isWithin(player.world, radius, getBlockAtFeet(player));
    }

    /**
     * Called if both {@link NearBlockManipulator#amount} and {@link NearBlockManipulator#radius} are variable
     * @param player The player
     * @return 1 if the block matches, 0 otherwise
     */
    private int variableAmountVariableRadius(final PlayerEntity player) {
        radius.setCurrentNeedValue(parent, player);
        return getCountWithin(player.world, radius.apply(player), getBlockAtFeet(player));
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
    private int isWithin(final World world, final double radius, final BlockPos center) {
        if (radius < 0) return 0;

        for (double x = -radius; x <= radius; x += 1d) {
            for (double y = -radius; y <= radius; y += 1d) {
                for (double z = -radius; z <= radius; z += 1d) {
                    if (matchFn.apply(world.getBlockState(center.add(x, y, z)))) return 1;
                }
            }
        }

        return 0;
    }

    /**
     * Checks how many blocks within the given radius match our predicate; iterates the entire radius
     * @param world  The {@link World} to check
     * @param radius The radius to check within
     * @param center The {@link BlockPos} at the center
     * @return The number of matching blocks
     */
    private int getCountWithin(final World world, final double radius, final BlockPos center) {
        if (radius < 0) return 0;

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

    /**
     * Iterates the block list and removes any conditions that will never match a block.
     */
    private void checkBlockList() {
        // Optimize the list to only predicates that will match:
        final IForgeRegistry<Block> blockReg = RegistryManager.ACTIVE.getRegistry(Block.class);
        final Iterator<IBlockPredicate> iter = blocks.iterator();

        /*
            TODO: There are probably extra optimizations that can be made here, including
            precompiling a HashSet of states that match when comparing against larger lists of predicates
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

            if (matches) {
                continue;
            }

            NeedsMod.LOGGER.info("Predicate will match no blocks; it will be removed");
            iter.remove();
        }
    }

    /**
     * Sets the matching function based on if any conditions will match void
     * @throws IllegalArgumentException If there are no blocks.
     */
    private void setMatchingFunction() throws IllegalArgumentException {
        if (blocks.size() <= 0) {
            throw new IllegalArgumentException("On/near block manipulator has no predicates that will match blocks.");
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
    }
}

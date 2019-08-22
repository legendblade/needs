package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.CountedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.blocks.BlockStatePredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.IBlockPredicate;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "A collection of manipulators that check blocks.")
public abstract class BlockCheckingManipulator extends TooltipManipulator implements ITrigger, ICondition {
    @Expose
    @Document(description = "The amount to apply; when this is checking an area, count will refer to the number of matching blocks in the area")
    protected CountedExpressionContext amount;

    @Expose
    @Document(type = IBlockPredicate.class, description = "A list of blocks to check")
    protected List<IBlockPredicate> blocks = Collections.emptyList();
    protected ITriggerable parentCondition;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        validateCommon();
        super.validate(need);
    }

    @Override
    public void validateCondition(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onLoaded() {
        onLoadedCommon();
        super.onLoaded();
    }

    @Override
    public void onConditionLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        onLoadedCommon();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        onLoadedCommon();
    }

    @Nullable
    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        final Block block = Block.getBlockFromItem(item.getItem());

        for (final IBlockPredicate predicate : blocks) {
            if(predicate instanceof BlockStatePredicate) {
                if (block.getStateContainer().getValidStates().stream().anyMatch(predicate)) return amount;
            } else {
                if (predicate.test(block.getDefaultState())) return amount;
            }
        }

        return null;
    }

    @Override
    protected void setupExpression(final Supplier<Double> currentValue, final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        super.setupExpression(currentValue, player, item, expr);
        amount.setIfRequired(CountedExpressionContext.COUNT, () -> 1d);
    }

    /**
     * Checks if the {@link BlockState} matches our predicates
     * @param target The state
     * @return True if it does, false otherwise
     */
    protected boolean isMatch(final BlockState target) {
        for (final IBlockPredicate predicate : blocks) {
            if (predicate.test(target)) return true;
        }
        return false;
    }

    protected void validateCommon() {
        if (blocks == null || blocks.isEmpty()) throw new IllegalArgumentException("An array of blocks must be specified.");
    }

    protected void onLoadedCommon() {
        if (amount != null) amount.build();
    }
}

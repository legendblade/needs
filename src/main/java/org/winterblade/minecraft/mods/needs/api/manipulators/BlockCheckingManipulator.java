package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
public abstract class BlockCheckingManipulator extends TooltipManipulator {
    @Expose
    @Document(description = "The amount to apply; when this is checking an area, count will refer to the number of matching blocks in the area")
    protected CountedExpressionContext amount;

    @Expose
    @Document(type = IBlockPredicate.class, description = "A list of blocks to check")
    protected List<IBlockPredicate> blocks = Collections.emptyList();

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        if (blocks == null || blocks.isEmpty()) throw new IllegalArgumentException("An array of blocks must be specified.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        amount.syncAll();
        super.onLoaded();
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
}

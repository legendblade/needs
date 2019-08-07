package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.util.blocks.BlockStatePredicate;
import org.winterblade.minecraft.mods.needs.util.blocks.IBlockPredicate;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Document(description = "A collection of manipulators that check blocks.")
public abstract class BlockCheckingManipulator <T extends NeedExpressionContext> extends TooltipManipulator {
    @Expose
    @Document(description = "The amount to apply")
    protected T amount;

    @Expose
    @Document(type = IBlockPredicate.class, description = "A list of blocks to check")
    protected List<IBlockPredicate> blocks = Collections.emptyList();

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

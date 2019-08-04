package org.winterblade.minecraft.mods.needs.util.blocks;

import net.minecraft.block.BlockState;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class BlockStatePredicate implements IBlockPredicate {
    private final Predicate<BlockState> predicate;
    private final int testCount;

    public BlockStatePredicate(final Predicate<BlockState> predicate, final int testCount) {
        this.predicate = predicate;
        this.testCount = testCount;
    }

    @Override
    public boolean test(final BlockState blockState) {
        return predicate.test(blockState);
    }

    @Override
    public int compareTo(@Nonnull final IBlockPredicate o) {
        // Sort behind simple predicates, then by test count
        if (o instanceof SimpleBlockPredicate) return 1;
        if (!(o instanceof BlockStatePredicate)) return -1;

        return Integer.compare(this.testCount, ((BlockStatePredicate) o).testCount);
    }
}

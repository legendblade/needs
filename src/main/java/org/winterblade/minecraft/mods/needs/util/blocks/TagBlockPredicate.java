package org.winterblade.minecraft.mods.needs.util.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

@SuppressWarnings("WeakerAccess")
public class TagBlockPredicate implements IBlockPredicate {
    private final ResourceLocation tagLoc;

    public TagBlockPredicate(final String tagString) {
        this.tagLoc = new ResourceLocation(tagString.substring(4));
    }

    @Override
    public boolean test(final BlockState block) {
        return getTag().contains(block.getBlock());
    }

    @Override
    public int compareTo(@Nonnull final IBlockPredicate o) {
        // Sorted behind everything
        if (!(o instanceof TagBlockPredicate)) return -1;
        return Integer.compare(getTag().getAllElements().size(), ((TagBlockPredicate) o).getTag().getAllElements().size());
    }

    protected Tag<Block> getTag() {
        return BlockTags.getCollection().getOrCreate(tagLoc);
    }
}

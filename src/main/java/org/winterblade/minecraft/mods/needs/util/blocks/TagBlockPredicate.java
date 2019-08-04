package org.winterblade.minecraft.mods.needs.util.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

@SuppressWarnings("WeakerAccess")
public class TagBlockPredicate implements IBlockPredicate {
    private final ResourceLocation tag;

    public TagBlockPredicate(final String tagString) {
        this.tag = new ResourceLocation(tagString.substring(4));
    }

    @Override
    public boolean test(final BlockState block) {
        return block.getBlock().getTags().contains(tag);
    }

    @Override
    public int compareTo(@Nonnull final IBlockPredicate o) {
        // Sorted behind everything
        if (!(o instanceof TagBlockPredicate)) return -1;
        return 0; //Integer.compare(getTag().getAllElements().size(), ((TagBlockPredicate) o).getTag().getAllElements().size());
    }
}

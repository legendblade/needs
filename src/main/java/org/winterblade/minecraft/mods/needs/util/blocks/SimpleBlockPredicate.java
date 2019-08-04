package org.winterblade.minecraft.mods.needs.util.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nonnull;

@SuppressWarnings("WeakerAccess")
public class SimpleBlockPredicate implements IBlockPredicate {

    private final Block block;

    public SimpleBlockPredicate(final String input) {
        this(RegistryManager.ACTIVE.getRegistry(Block.class).getValue(new ResourceLocation(input)));
    }

    public SimpleBlockPredicate(final Block block) {
        this.block = block;
    }

    @Override
    public boolean test(final BlockState block) {
        return block.getBlock().equals(this.block);
    }

    @Override
    public int compareTo(@Nonnull final IBlockPredicate o) {
        // Move these towards the top
        return (o instanceof SimpleBlockPredicate) ? 0 : -1;
    }
}

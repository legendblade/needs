package org.winterblade.minecraft.mods.needs.util.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

public class TagIngredient implements IIngredient {

    private final ResourceLocation tagLoc;

    public TagIngredient(final ResourceLocation tagLoc) {
        this.tagLoc = tagLoc;
    }

    @Override
    public boolean test(final ItemStack stack) {
        final Tag<Item> tag = ItemTags.getCollection().get(this.tagLoc);
        return tag != null && tag.contains(stack.getItem());
    }
}

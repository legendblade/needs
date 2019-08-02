package org.winterblade.minecraft.mods.needs.util.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.Tag;

public class TagIngredient implements IIngredient {

    private final Tag<Item> tag;

    public TagIngredient(final Tag<Item> tag) {
        this.tag = tag;
    }

    @Override
    public boolean isMatch(final ItemStack stack) {
        return tag.contains(stack.getItem());
    }
}

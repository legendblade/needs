package org.winterblade.minecraft.mods.needs.util.items;

import net.minecraft.item.ItemStack;

public class ItemIngredient implements IIngredient {
    private final ItemStack target;

    public ItemIngredient(final ItemStack target) {
        this.target = target;
    }


    @Override
    public boolean test(final ItemStack stack) {
        return target.isItemEqualIgnoreDurability(stack);
    }
}

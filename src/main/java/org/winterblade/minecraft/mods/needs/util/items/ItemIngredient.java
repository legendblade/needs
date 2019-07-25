package org.winterblade.minecraft.mods.needs.util.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemIngredient implements IIngredient {
    private final ItemStack target;

    public ItemIngredient(ItemStack target) {
        this.target = target;
    }


    @Override
    public boolean isMatch(ItemStack stack) {
        return target.isItemEqualIgnoreDurability(stack);
    }
}

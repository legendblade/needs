package org.winterblade.minecraft.mods.needs.util.items;

import net.minecraft.item.ItemStack;

public interface IIngredient {
    boolean isMatch(ItemStack stack);
}

package org.winterblade.minecraft.mods.needs.util;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.UUID;

public class ModifierUtils {
    /**
     * Gets the current modifier, removing it if it exists
     * @param inst       The attribute instance
     * @param modifierId The modifier UUID
     * @return  The value of the modifier, or 0 if it doesn't exist
     */
    public static double getCurrentModifier(IAttributeInstance inst, final UUID modifierId) {
        //noinspection ConstantConditions - Rule #1: Mojang Lies.
        if (inst == null) return 0;

        // Check if the modifier exists:
        final AttributeModifier mod = inst.getModifier(modifierId);
        if (mod == null) return 0;

        inst.removeModifier(mod); // Mojang, you're doing this as a map, pls. Why is by ID _worse_ than by value?
        return mod.getAmount();
    }

    /**
     * Set the modifier on the player
     * @param inst        The attribute instance
     * @param modifierId  The modifier ID
     * @param modfierName The modifier name
     * @param amount      The amount to apply
     * @param op          The type of the modifier
     */
    public static void setModifier(final IAttributeInstance inst, final UUID modifierId, final String modfierName, final double amount, final AttributeModifier.Operation op) {
        inst.applyModifier(new AttributeModifier(modifierId, modfierName, amount, op));
    }
}

package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;

import java.util.UUID;

public class MaxHealthNeed extends Need {
    private static final UUID healthModifier = UUID.fromString("96423cbe-16b0-412a-8a2b-7a6a9872f56d");
    private static final String healthModifierName = NeedsMod.MODID + ".MaxHPModifier";

    @Override
    public String getName() {
        return "Max Health";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 1;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return Float.MAX_VALUE;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.getMaxHealth();
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }

    @Override
    protected double setValue(final PlayerEntity player, final double newValue, final double adjust) {
        final IAttributeInstance inst = player.getAttribute(SharedMonsterAttributes.MAX_HEALTH);

        //noinspection ConstantConditions - Rule #1: Mojang Lies.
        if (inst == null) return getValue(player);

        // Check if the modifier exists:
        final AttributeModifier mod = inst.getModifier(healthModifier);
        final double prev;
        if (mod != null) {
            inst.removeModifier(mod); // Mojang, you're doing this as a map, pls. Why is by ID _worse_ than by value?
            prev = mod.getAmount();
        } else prev = 0;

        // If we're at 0 now, just bail, don't set a new modifier
        if (adjust + prev == 0) return getValue(player);

        // Apply the new modifier
        inst.applyModifier(new AttributeModifier(healthModifier, healthModifierName, adjust + prev, AttributeModifier.Operation.ADDITION));

        final double result = getValue(player);

        // Fixup the player's health to match the new min/max
        if (0 < adjust) player.heal((float) adjust);
        else if(result < player.getHealth()) player.setHealth((float) result);

        return result;
    }
}

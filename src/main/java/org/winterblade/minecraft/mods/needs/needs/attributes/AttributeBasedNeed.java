package org.winterblade.minecraft.mods.needs.needs.attributes;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.CachedTickingNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public abstract class AttributeBasedNeed extends CachedTickingNeed {
    protected final IAttribute attribute;
    protected final UUID modifierId;
    protected final String modifierName;

    protected AttributeBasedNeed(final IAttribute attribute, final UUID modifierId, final String modifierName) {
        this.attribute = attribute;
        this.modifierId = modifierId;
        this.modifierName = modifierName;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.getAttribute(attribute).getValue();
    }

    @Override
    protected double setValue(final PlayerEntity player, final double newValue, final double adjust) {
        final IAttributeInstance inst = player.getAttribute(attribute);

        //noinspection ConstantConditions - Rule #1: Mojang Lies.
        if (inst == null) return getValue(player);

        // Check if the modifier exists:
        final AttributeModifier mod = inst.getModifier(modifierId);
        final double prev;
        if (mod != null) {
            inst.removeModifier(mod); // Mojang, you're doing this as a map, pls. Why is by ID _worse_ than by value?
            prev = mod.getAmount();
        } else prev = 0;

        // If we're at 0 now, just bail, don't set a new modifier
        if (adjust + prev == 0) return getValue(player);

        // Apply the new modifier
        inst.applyModifier(new AttributeModifier(modifierId, modifierName, adjust + prev, AttributeModifier.Operation.ADDITION));

        final double result = getValue(player);

        if (attribute == SharedMonsterAttributes.MAX_HEALTH) {
            // Fixup the player's health to match the new min/max
            if (0 < adjust) player.heal((float) adjust);
            else if (result < player.getHealth()) player.setHealth((float) result);
        }

        return result;
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }
}

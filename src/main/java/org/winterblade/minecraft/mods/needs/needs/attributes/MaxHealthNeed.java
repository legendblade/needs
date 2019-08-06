package org.winterblade.minecraft.mods.needs.needs.attributes;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.util.UUID;

public class MaxHealthNeed extends AttributeBasedNeed {
    private static final UUID healthModifier = UUID.fromString("96423cbe-16b0-412a-8a2b-7a6a9872f56d");
    private static final String healthModifierName = NeedsMod.MODID + ".MaxHPModifier";

    public MaxHealthNeed() {
        super(SharedMonsterAttributes.MAX_HEALTH, healthModifier, healthModifierName);
    }

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
}

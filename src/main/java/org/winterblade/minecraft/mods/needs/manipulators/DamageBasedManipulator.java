package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "A collection of manipulators based on damage (in or out)")
public abstract class DamageBasedManipulator extends BaseManipulator {
    @Expose
    @Document(description = "The amount to change by when triggered")
    protected DamageExpressionContext amount;

    @Expose
    @OptionalField(defaultValue = "True")
    @Document(description = "If hostile mobs should trigger this; if inbound damage, will check the source, if outbound, will check the target")
    protected boolean hostile = true;

    @Expose
    @OptionalField(defaultValue = "True")
    @Document(description = "If passive mobs should trigger this; if inbound damage, will check the source, if outbound, will check the target")
    protected boolean passive = true;

    @Expose
    @OptionalField(defaultValue = "True")
    @Document(description = "If players should trigger this; if inbound damage, will check the source, if outbound, will check the target")
    protected boolean players = true;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The minimum amount of damage required to trigger this")
    protected double minAmount = Double.NEGATIVE_INFINITY;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The maximum amount of damage required to trigger this")
    protected double maxAmount = Double.POSITIVE_INFINITY;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean doesNotMatchFilters(final Entity target) {
        // TODO: There are probably edge cases that aren't covered here:
        return (!players && target instanceof PlayerEntity)
                || (!hostile && target instanceof MonsterEntity)
                || (!passive && (target instanceof AnimalEntity
                    || target instanceof AmbientEntity
                    || target instanceof AbstractVillagerEntity));
    }
}

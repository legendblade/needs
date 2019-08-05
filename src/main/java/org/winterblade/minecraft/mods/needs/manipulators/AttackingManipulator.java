package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;

public class AttackingManipulator extends DamageBasedManipulator {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    protected void onAttacking(final LivingDamageEvent event) {
        if (!(event.getSource() instanceof EntityDamageSource)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;

        final EntityDamageSource source = (EntityDamageSource) event.getSource();
        if (!(source.getTrueSource() instanceof PlayerEntity)) return;

        final PlayerEntity player = (PlayerEntity) source.getTrueSource();
        final Entity target = event.getEntity();

        // TODO: There are probably edge cases that aren't covered here:
        if ((!players && target instanceof PlayerEntity)
                || (!hostile && target instanceof MonsterEntity)
                || (!passive && (target instanceof AnimalEntity
                        || target instanceof AmbientEntity
                        || target instanceof AbstractVillagerEntity))) return;

        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(player, amount.get(), this);
    }
}

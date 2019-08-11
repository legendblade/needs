package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DamageBasedManipulator;

@Document(description = "Triggered when the player attacks another entity.")
public class AttackingManipulator extends DamageBasedManipulator {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    protected void onAttacking(final LivingDamageEvent event) {
        if (!(event.getSource() instanceof EntityDamageSource)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;

        final EntityDamageSource source = (EntityDamageSource) event.getSource();
        if (!(source.getTrueSource() instanceof PlayerEntity)) return;

        final PlayerEntity player = (PlayerEntity) source.getTrueSource();
        final Entity target = event.getEntity();
        if (doesNotMatchFilters(target) || failsDimensionCheck(player)) return;

        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(player, amount.apply(player), this);
    }
}

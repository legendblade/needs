package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;

public class HealManipulator extends DamageBasedManipulator {
    @SubscribeEvent
    protected void onDamaged(final LivingHealEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;

        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(event.getEntity(), amount.get(), this);
    }
}

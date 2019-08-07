package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;

@Document(description = "Triggered when the player is healed")
public class HealManipulator extends DamageBasedManipulator {
    @SubscribeEvent
    protected void onDamaged(final LivingHealEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;

        final PlayerEntity player = (PlayerEntity) event.getEntity();

        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(player, amount.get(), this);
    }
}

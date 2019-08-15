package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.DamageBasedManipulator;

@Document(description = "Triggered when the player is healed")
public class HealManipulator extends DamageBasedManipulator {
    private float healing;

    @Override
    public void onLoaded() {
        super.onLoaded();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerLoaded(final ConditionalManipulator parent) {
        super.onTriggerLoaded(parent);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asTrigger);
    }

    @Override
    public void onTriggerUnloaded() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double) healing);
        return amount.apply(player);
    }

    private void asManipulator(final LivingHealEvent event) {
        if (failsMatch(event)) return;
        final PlayerEntity player = (PlayerEntity) event.getEntity();

        healing = event.getAmount();
        parent.adjustValue(player, getAmount(player), this);
    }

    private void asTrigger(final LivingHealEvent event) {
        if (failsMatch(event)) return;
        final PlayerEntity player = (PlayerEntity) event.getEntity();

        healing = event.getAmount();
        parentCondition.trigger(player, this);
    }

    private boolean failsMatch(final LivingHealEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return true;
        return event.getAmount() < minAmount || maxAmount < event.getAmount();
    }
}

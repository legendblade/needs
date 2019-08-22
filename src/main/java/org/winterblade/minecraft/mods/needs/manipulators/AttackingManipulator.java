package org.winterblade.minecraft.mods.needs.manipulators;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DamageBasedManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Document(description = "Triggered when the player attacks another entity.")
public class AttackingManipulator extends DamageBasedManipulator {
    private float damage;

    @Override
    public void onLoaded() {
        super.onLoaded();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::processManipulator);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        super.onTriggerLoaded(parentNeed, parentCondition);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::processTrigger);
    }

    @Override
    public void onTriggerUnloaded() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        return get(player);
    }

    /**
     * Process the event as a manipulator
     * @param event The event
     */
    private void processManipulator(final LivingDamageEvent event) {
        final PlayerEntity player = matchPlayer(event);
        if (player == null) return;

        damage = event.getAmount();
        parent.adjustValue(player, get(player), this);
    }

    /**
     * Process the event as a trigger
     * @param event The event
     */
    private void processTrigger(final LivingDamageEvent event) {
        final PlayerEntity player = matchPlayer(event);
        if (player == null) return;

        damage = event.getAmount();
        parentCondition.trigger(player, this);
    }

    /**
     * Set up the expression and get the value
     * @param player The player to test
     * @return The value of the expression
     */
    private double get(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double) damage);
        return amount.apply(player);
    }

    /**
     * Match the event and return the player if it passes
     * @param event The event
     * @return The player if the event matches, null otherwise.
     */
    @Nullable
    private PlayerEntity matchPlayer(@Nonnull final LivingDamageEvent event) {
        if (!(event.getSource() instanceof EntityDamageSource)) return null;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return null;

        final EntityDamageSource source = (EntityDamageSource) event.getSource();
        if (!(source.getTrueSource() instanceof PlayerEntity)) return null;

        final Entity target = event.getEntity();
        if (doesNotMatchFilters(target)) return null;

        return (PlayerEntity) source.getTrueSource();
    }
}

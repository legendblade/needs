package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.DamageBasedManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.Collections;
import java.util.List;

@Document(description = "Called when the player is hurt")
public class HurtManipulator extends DamageBasedManipulator {
    @Expose
    @OptionalField(defaultValue = "All")
    @Document(type = String.class, description = "An array of damage sources which will trigger this event.")
    protected List<String> sources = Collections.emptyList();

    private boolean shouldCheckSource = false;
    private float damage;

    @Override
    public void onLoaded() {
        super.onLoaded();
        loadCommon();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        super.onTriggerLoaded(parentNeed, parentCondition);
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
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double) damage);
        return amount.apply(player);
    }

    private void asManipulator(final LivingDamageEvent event) {
        if (failsMatch(event)) return;
        final PlayerEntity player = (PlayerEntity) event.getEntity();

        damage = event.getAmount();
        parent.adjustValue(player, getAmount(player), this);
    }

    private void asTrigger(final LivingDamageEvent event) {
        if (failsMatch(event)) return;
        final PlayerEntity player = (PlayerEntity) event.getEntity();

        damage = event.getAmount();
        parentCondition.trigger(player, this);
    }

    private boolean failsMatch(final LivingDamageEvent event) {
        return
            !(event.getEntity() instanceof PlayerEntity) ||
                (event.getAmount() < minAmount) ||
                (maxAmount < event.getAmount()) ||
                (shouldCheckSource &&
                    !sources.contains(event.getSource().damageType)) ||
                ((event.getSource() instanceof EntityDamageSource) &&
                    doesNotMatchFilters(event.getSource().getTrueSource()));
    }

    private void loadCommon() {
        shouldCheckSource = (0 < sources.size());
    }
}

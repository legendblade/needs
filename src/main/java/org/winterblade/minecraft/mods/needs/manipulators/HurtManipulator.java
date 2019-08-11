package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DamageBasedManipulator;

import java.util.Collections;
import java.util.List;

@Document(description = "Called when the player is hurt")
public class HurtManipulator extends DamageBasedManipulator {
    @Expose
    @OptionalField(defaultValue = "All")
    @Document(type = String.class, description = "An array of damage sources which will trigger this event.")
    protected List<String> sources = Collections.emptyList();

    protected boolean shouldCheckSource = false;

    @Override
    public void onLoaded() {
        super.onLoaded();
        shouldCheckSource = (0 < sources.size());
    }

    @SubscribeEvent
    protected void onDamaged(final LivingDamageEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;
        if (shouldCheckSource && !sources.contains(event.getSource().damageType)) return;
        if (event.getSource() instanceof EntityDamageSource && doesNotMatchFilters(event.getSource().getTrueSource())) return;

        final PlayerEntity player = (PlayerEntity) event.getEntity();
        if (failsDimensionCheck(player)) return;

        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(player, amount.get(), this);
    }
}

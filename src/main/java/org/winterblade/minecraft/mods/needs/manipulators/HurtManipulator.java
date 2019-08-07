package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;

import java.util.Collections;
import java.util.List;

public class HurtManipulator extends DamageBasedManipulator {
    @Expose
    @Document(type = String.class)
    protected List<String> sources = Collections.emptyList();

    protected boolean shouldCheckSource = false;

    @Override
    public void onCreated() {
        super.onCreated();
        if (0 < sources.size()) shouldCheckSource = true;
    }

    @SubscribeEvent
    protected void onDamaged(final LivingDamageEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        if (event.getAmount() < minAmount || maxAmount < event.getAmount()) return;
        if (shouldCheckSource && !sources.contains(event.getSource().damageType)) return;

        final PlayerEntity player = (PlayerEntity) event.getEntity();

        amount.setCurrentNeedValue(parent, player);
        amount.setIfRequired(DamageExpressionContext.AMOUNT, () -> (double)event.getAmount());
        parent.adjustValue(player, amount.get(), this);
    }
}

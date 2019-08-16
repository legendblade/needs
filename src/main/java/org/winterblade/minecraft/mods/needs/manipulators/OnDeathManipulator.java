package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered when the player dies.")
public class OnDeathManipulator extends BaseManipulator implements ITrigger {
    @Expose
    @Document(description = "The amount to change by.")
    private NeedExpressionContext amount;
    private ConditionalManipulator parentCondition;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        // Nothing?
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
        amount.build();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asTrigger);
        if (amount != null) amount.build();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerUnloaded() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player);
    }

    private void asManipulator(final LivingDeathEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof PlayerEntity) || event.isCanceled()) return;
        final PlayerEntity pl = (PlayerEntity) event.getEntity();
        parent.adjustValue(pl, getAmount(pl), this);
    }

    private void asTrigger(final LivingDeathEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof PlayerEntity) || event.isCanceled()) return;
        final PlayerEntity pl = (PlayerEntity) event.getEntity();
        parentCondition.trigger(pl, this);
    }
}

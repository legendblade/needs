package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered on tick")
public class TickManipulator extends BaseManipulator implements ITrigger {
    @Expose
    @Document(description = "The amount to adjust by; multiplied by the number of ticks between checks")
    protected NeedExpressionContext amount;

    private ITriggerable parentCondition;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asManipulator);
        super.onLoaded();
        amount.build();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asTrigger);
        if (amount != null) amount.build();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerUnloaded() {
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player) * TickManager.INSTANCE.getBucketCount();
    }

    private void asManipulator(final PlayerEntity player) {
        parent.adjustValue(player, getAmount(player), this);
    }

    private void asTrigger(final PlayerEntity player) {
        parentCondition.trigger(player, this);
    }
}

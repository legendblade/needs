package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DimensionBasedManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered on tick")
public class TickManipulator extends DimensionBasedManipulator {
    @Expose
    @Document(description = "The amount to adjust by; multiplied by the number of ticks between checks")
    protected NeedExpressionContext amount;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
        super.onLoaded();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    private void onTick(final PlayerEntity player) {
        if (failsDimensionCheck(player)) return;

        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.apply(player) * TickManager.INSTANCE.getBucketCount(), this);
    }
}

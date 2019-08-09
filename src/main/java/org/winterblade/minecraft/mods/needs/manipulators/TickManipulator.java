package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

@Document(description = "Triggered on tick")
public class TickManipulator extends BaseManipulator {
    @Expose
    @Document(description = "The amount to adjust by; multiplied by the number of ticks between checks")
    protected NeedExpressionContext amount;

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this::onTick);
    }

    private void onTick(final PlayerEntity player) {
        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.get() * TickManager.INSTANCE.getBucketCount(), this);
    }
}

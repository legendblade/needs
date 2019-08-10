package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.Set;

@Document(description = "Triggered on tick")
public class TickManipulator extends BaseManipulator {
    @Expose
    @Document(description = "The amount to adjust by; multiplied by the number of ticks between checks")
    protected NeedExpressionContext amount;

    @Expose
    @Document(description = "Optional list of dimension IDs to limit the effect to.")
    @OptionalField(defaultValue = "All Dimensions")
    protected Set<Integer> dimensions;

    private boolean checkDims;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
        checkDims = !dimensions.isEmpty();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    private void onTick(final PlayerEntity player) {
        if (checkDims && !dimensions.contains(player.world.getDimension().getType().getId())) return;

        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.get() * TickManager.INSTANCE.getBucketCount(), this);
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.Collections;
import java.util.Set;

@Document(description = "When used as a manipulator or trigger, triggers on tick as long as the player is " +
        "in the given dimension(s).\n\n" +
        "When used as a condition, will pass if the player is in the given dimension(s).")
public class DimensionBasedManipulator extends BaseManipulator implements ICondition, ITrigger {
    @Expose
    @Document(description = "The amount to affect the need by; optional if not a manipulator.")
    @OptionalField(defaultValue = "None")
    protected NeedExpressionContext amount;

    @Expose
    @Document(description = "List of dimension IDs to limit the effect to.")
    protected Set<Integer> dimensions = Collections.emptySet();

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (amount == null) throw new IllegalArgumentException("Amount must be specified when used as a manipulator.");
        validate();
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        amount.build();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, (player) -> {
            if (!test(player)) return;
            parent.adjustValue(player, get(player), this);
        });
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void validateCondition(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        validate();
    }

    @Override
    public void onConditionLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parent = parentNeed;
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        return get(player);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        return dimensions.contains(player.world.getDimension().getType().getId());
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ConditionalManipulator parentCondition) {
        validate();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parent = parentNeed;
        if (amount != null) amount.build();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, (player) -> {
            if (!test(player)) return;
            parentCondition.trigger(player, this);
        });
    }

    @Override
    public void onTriggerUnloaded() {
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    private void validate() throws IllegalArgumentException {
        if (dimensions.isEmpty()) throw new IllegalArgumentException("You must have at least one dimension.");
    }

    private double get(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player);
    }
}

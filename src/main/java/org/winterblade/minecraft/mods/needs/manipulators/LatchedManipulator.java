package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.capabilities.latch.ILatchedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.latch.LatchedCapability;
import org.winterblade.minecraft.mods.needs.expressions.ConditionalExpressionContext;
import org.winterblade.minecraft.mods.needs.expressions.OrConditionalExpressionContext;

@Document(description = "Creates a latched condition which only passes the first time a condition is met, and will only " +
        "reset when the associated condition is no longer met.\n\n" +
        "Note that if using this as a trigger or a condition, the latch will still reset regardless of if the " +
        "manipulator as a whole activates.\n\n" +
        "Note too (as well) and two (secondly) that triggers on the condition(s) under this one will act in addition to " +
        "the constant on-tick behavior when used as a trigger/manipulator; to avoid it ticking, use it as a condition " +
        "instead with a different trigger.")
public class LatchedManipulator extends BaseManipulator implements ICondition, ITrigger, ITriggerable {
    @Expose
    @Document(description = "An ID for the latch; if two latches share the same name, they will share their state. Be " +
            "careful if using the same latch ID in a subcondition, as you might produce a latch that flips on and off " +
            "every other tick.")
    private String id;

    @Expose
    @Document(description = "The amount to apply when this latch triggers; `source` is only valid when used with a " +
            "condition that has triggers.")
    @OptionalField(defaultValue = "matchedValue")
    private OrConditionalExpressionContext amount;

    @Expose
    @Document(description = "The amount to apply when this latch is no longer being triggered; `source` is only valid " +
            "when used with a condition that has triggers")
    @OptionalField(defaultValue = "-amount")
    private OrConditionalExpressionContext unlatchAmount;

    @Expose
    @Document(description = "The associated condition to check.")
    private ICondition condition;

    private boolean isUnlatch = false;
    private ITriggerable parentCondition;
    private ITrigger lastSource;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        validateCommon();
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void validateCondition(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        loadCommon();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asManipulator);
    }

    @Override
    public void onConditionLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        loadCommon();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        loadCommon();
        this.parentCondition = parentCondition;
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asTrigger);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        condition.onConditionUnloaded();
    }

    @Override
    public void onConditionUnloaded() {
        condition.onConditionUnloaded();
    }

    @Override
    public void onTriggerUnloaded() {
        condition.onConditionUnloaded();
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (isUnlatch) {
            if (unlatchAmount != null) return getAmount(player, unlatchAmount);
            if (amount != null) return -getAmount(player, amount);
            return -condition.getAmount(player);
        }

        return amount != null ? getAmount(player, amount) : condition.getAmount(player);
    }

    @Override
    public boolean test(final PlayerEntity playerEntity) {
        // Get our capability
        final ILatchedCapability theCap = playerEntity
                .getCapability(LatchedCapability.CAPABILITY, null)
                .orElseGet(LatchedCapability::new);

        final boolean conditionResult = condition.test(playerEntity);
        final boolean previousResult = theCap.lastValue(id);

        if (conditionResult == previousResult) return false;

        isUnlatch = previousResult;
        theCap.setValue(id, conditionResult);
        return true;
    }

    @Override
    public void trigger(final PlayerEntity player, final ITrigger trigger) {
        lastSource = trigger;
        if (parentCondition != null) {
            parentCondition.trigger(player, this);
            return;
        }

        if (!test(player)) return;

        parent.adjustValue(player, getAmount(player), this);
        lastSource = null;
    }

    protected void asManipulator(final PlayerEntity player) {
        if (!test(player)) return;
        parent.adjustValue(player, getAmount(player), this);
    }

    protected void asTrigger(final PlayerEntity player) {
        trigger(player, this);
    }

    protected double getAmount(final PlayerEntity player, final ConditionalExpressionContext expr) {
        expr.setCurrentNeedValue(parent, player);
        expr.setIfRequired(ConditionalExpressionContext.MATCHED_VALUE, () -> condition.getAmount(player));
        expr.setIfRequired(OrConditionalExpressionContext.SOURCE, () -> lastSource != null ? lastSource.getAmount(player) : 0);
        return expr.apply(player);
    }

    private void validateCommon() throws IllegalArgumentException {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("An ID must be specified.");
        if (condition == null) throw new IllegalArgumentException("A condition to check must be specified.");
    }

    private void loadCommon() {
        if (amount != null) amount.build();
        if (unlatchAmount != null) unlatchAmount.build();
        condition.onConditionLoaded(parent, null);
    }
}

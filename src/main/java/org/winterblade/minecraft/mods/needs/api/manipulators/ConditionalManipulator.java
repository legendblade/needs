package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.OrConditionalExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Document(description = "Represents a series of conditions that will be tested one after another until the first one " +
        "returns true.")
public class ConditionalManipulator extends BaseManipulator implements ICondition {
    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The conditions to check when this manipulator is triggered")
    private List<ICondition> conditions = Collections.emptyList();

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The conditions which will trigger this manipulator. Triggers should be placed on the top " +
            "level condition for clarity, but triggers on inner conditions will propagate up to the top most condition " +
            "before being tested.\n\n" +
            "The root condition must have at least one trigger, even if there are triggers on inner conditions.")
    private List<ITrigger> triggers = Collections.emptyList();

    @Expose
    @Document(description = "The amount to adjust the associated need by; if omitted, this will default to the expression " +
            "variable `matchedValue`, which will be the value of the first matching condition. If that condition doesn't " +
            "return a value, the need will not be affected.\n\n" +
            "Similarly, if `source` is used and the associated trigger doesn't have an amount and is used, it will return 0.")
    @OptionalField(defaultValue = "matchedValue")
    private NeedExpressionContext amount;

    private ConditionalManipulator parentCondition;

    private boolean getMatch;

    // We only get away with this because MC is single-threaded. Otherwise, this'd have to go into a thread context
    private double lastMatch;
    private ITrigger lastSource;

    /**
     * Validate as a manipulator
     * @param need The parent need
     * @throws IllegalArgumentException If anything fails validation
     */
    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        validateCondition(this);
    }

    /**
     * Validate as a condition
     * @param parent The parent condition
     */
    @Override
    public void validateCondition(final ConditionalManipulator parent) throws IllegalArgumentException {
        if (conditions.isEmpty()) throw new IllegalArgumentException("There must be at least one condition.");
        if (parent == null && triggers.isEmpty()) throw new IllegalArgumentException("Root conditions must have at least one trigger.");
        conditions.forEach((c) -> c.validateCondition(this));
        triggers.forEach((c) -> c.validateTrigger(this));
    }

    /**
     * Load as a manipulator
     */
    @Override
    public void onLoaded() {
        super.onLoaded();
        onConditionLoaded(null);
    }

    /**
     * Load as a condition
     * @param parent The parent condition
     */
    @Override
    public void onConditionLoaded(@Nullable final ConditionalManipulator parent) {
        this.parentCondition = parent;
        conditions.forEach(c -> c.onConditionLoaded(this));
        triggers.forEach(t -> t.onTriggerLoaded(this));
        getMatch = amount == null || amount.isRequired(OrConditionalExpressionContext.MATCHED_VALUE);
    }

    @Override
    public void onUnloaded() {
        onConditionUnloaded();
    }

    /**
     * Unload as a manipulator or condition
     */
    @Override
    public void onConditionUnloaded() {
        super.onUnloaded();
        conditions.forEach(ICondition::onConditionUnloaded);
        triggers.forEach(ITrigger::onTriggerUnloaded);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        lastMatch = 0;
        for (final ICondition condition : conditions) {
            if (condition.test(player)) {
                if (getMatch) lastMatch = condition.getAmount(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return lastMatch;
        amount.setIfRequired(OrConditionalExpressionContext.MATCHED_VALUE, () -> lastMatch);
        amount.setIfRequired(OrConditionalExpressionContext.SOURCE, () -> lastSource.getAmount(player));
        return amount.apply(player);
    }

    /**
     * Triggers the {@link ConditionalManipulator} to check its conditions
     * @param player The player to test
     */
    public void trigger(final PlayerEntity player, final ITrigger trigger) {
        lastSource = trigger;

        // Delegate this up to the parent condition
        if (parentCondition != null) {
            parentCondition.trigger(player, trigger);
            return;
        }

        if (!test(player)) return;

        parent.adjustValue(player, getAmount(player), this);
        lastSource = null;
    }
}

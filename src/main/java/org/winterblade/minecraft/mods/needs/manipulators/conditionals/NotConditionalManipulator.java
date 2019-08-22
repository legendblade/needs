package org.winterblade.minecraft.mods.needs.manipulators.conditionals;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.expressions.OrConditionalExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nullable;

@Document(description = "Inverts the given condition.")
public class NotConditionalManipulator extends ConditionalManipulator {
    @Expose
    @Document(description = "The amount to adjust the associated need by; if omitted, this will default to the expression " +
            "variable `matchedValue`, which will be the value of the condition. If that condition doesn't " +
            "return a value, the need will not be affected.\n\n" +
            "Similarly, if `source` is used and the associated trigger doesn't have an amount and is used, it will return 0.")
    @OptionalField(defaultValue = "matchedValue")
    protected OrConditionalExpressionContext amount;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The condition to invert the check for")
    private ICondition condition;

    private boolean getMatch;

    /**
     * Validate as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void validateCondition(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        super.validateCondition(parentNeed, parentCondition);
        if (condition == null) throw new IllegalArgumentException("Condition must be specified.");
        condition.validateCondition(parentNeed, this);
    }

    /**
     * Load as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void onConditionLoaded(final Need parentNeed, @Nullable final ITriggerable parentCondition) {
        super.onConditionLoaded(parentNeed, parentCondition);
        condition.onConditionLoaded(parentNeed, this);
        getMatch = amount == null || amount.isRequired(OrConditionalExpressionContext.MATCHED_VALUE);
    }

    /**
     * Unload as a manipulator or condition
     */
    @Override
    public void onConditionUnloaded() {
        super.onConditionUnloaded();
        condition.onConditionUnloaded();
    }

    @Override
    public boolean test(final PlayerEntity player) {
        lastMatch = 0;
        if (condition.test(player)) return false;

        if (getMatch) lastMatch = condition.getAmount(player);
        return true;
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return lastMatch;
        amount.setIfRequired(OrConditionalExpressionContext.MATCHED_VALUE, () -> lastMatch);
        amount.setIfRequired(OrConditionalExpressionContext.SOURCE, () -> lastSource.getAmount(player));
        return amount.apply(player);
    }
}

package org.winterblade.minecraft.mods.needs.manipulators.conditionals;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.expressions.OrConditionalExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Document(description = "Represents a series of conditions that will be tested one after another until the first one " +
        "returns true.")
public class OrConditionalManipulator extends ConditionalManipulator {
    @Expose
    @Document(description = "The amount to adjust the associated need by; if omitted, this will default to the expression " +
            "variable `matchedValue`, which will be the value of the first matching condition. If that condition doesn't " +
            "return a value, the need will not be affected.\n\n" +
            "Similarly, if `source` is used and the associated trigger doesn't have an amount and is used, it will return 0.")
    @OptionalField(defaultValue = "matchedValue")
    protected OrConditionalExpressionContext amount;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The conditions to check when this manipulator is triggered", type = ICondition.class)
    protected List<ICondition> conditions = Collections.emptyList();

    protected boolean getMatch;

    /**
     * Validate as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void validateCondition(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        super.validateCondition(parentNeed, parentCondition);
        if (conditions.isEmpty()) throw new IllegalArgumentException("There must be at least one condition.");
        conditions.forEach((c) -> c.validateCondition(parentNeed, this));
    }

    /**
     * Load as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void onConditionLoaded(final Need parentNeed, @Nullable final ConditionalManipulator parentCondition) {
        super.onConditionLoaded(parentNeed, parentCondition);
        conditions.forEach(c -> c.onConditionLoaded(parentNeed, this));
        getMatch = amount == null || amount.isRequired(OrConditionalExpressionContext.MATCHED_VALUE);
        if (amount != null) amount.syncAll();
    }

    /**
     * Unload as a manipulator or condition
     */
    @Override
    public void onConditionUnloaded() {
        super.onUnloaded();
        conditions.forEach(ICondition::onConditionUnloaded);
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
}

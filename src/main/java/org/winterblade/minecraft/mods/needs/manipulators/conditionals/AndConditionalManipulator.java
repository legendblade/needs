package org.winterblade.minecraft.mods.needs.manipulators.conditionals;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.expressions.AndConditionalExpressionContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@Document(description = "Represents a series of conditions that will be tested one after another until the first one " +
        "returns false; applying itself if all conditions pass.")
public class AndConditionalManipulator extends ConditionalManipulator {
    @Expose
    @Document(description = "The amount to adjust the associated need by.\n\n" +
            "If `source` is used and the associated trigger doesn't have an amount and is used, it will return 0.")
    protected AndConditionalExpressionContext amount;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The conditions to check when this manipulator is triggered; the key for each condition " +
            "can be used in the amount expression to retrieve the value of that condition.", type = ICondition.class)
    private Map<String, ICondition> conditions = Collections.emptyMap();

    private boolean getMatch;
    private final Set<Consumer<PlayerEntity>> args = new HashSet<>();

    /**
     * Validate as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void validateCondition(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        super.validateCondition(parentNeed, parentCondition);
        if (conditions.isEmpty()) throw new IllegalArgumentException("There must be at least one condition.");
        conditions.forEach((k, v) -> {
            if (amount != null) amount.addArgument(k);
            v.validateCondition(parentNeed, this);
        });

        if (amount != null) amount.build();
    }

    /**
     * Load as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void onConditionLoaded(final Need parentNeed, @Nullable final ConditionalManipulator parentCondition) {
        super.onConditionLoaded(parentNeed, parentCondition);
        conditions.forEach((key, c) -> {
            c.onConditionLoaded(parentNeed, this);
            if (amount == null || !amount.isRequired(key)) return;

            args.add((p) -> amount.setIfRequired(key, () -> c.getAmount(p)));
            getMatch = true;
        });

        if (amount == null) getMatch = false;
    }

    /**
     * Unload as a manipulator or condition
     */
    @Override
    public void onConditionUnloaded() {
        super.onConditionUnloaded();
        conditions.values().forEach(ICondition::onConditionUnloaded);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        return conditions.values().stream().allMatch(condition -> condition.test(player));
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return lastMatch;
        if (getMatch) args.forEach((arg) -> arg.accept(player));
        amount.setIfRequired(AndConditionalExpressionContext.SOURCE, () -> lastSource.getAmount(player));
        return amount.apply(player);
    }
}

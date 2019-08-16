package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public abstract class ConditionalManipulator extends BaseManipulator implements ICondition {
    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The conditions which will trigger this manipulator. Triggers should be placed on the top " +
            "level condition for clarity, but triggers on inner conditions will propagate up to the top most condition " +
            "before being tested.\n\n" +
            "The root condition must have at least one trigger, even if there are triggers on inner conditions.")
    protected List<ITrigger> triggers = Collections.emptyList();

    protected ConditionalManipulator parentCondition;

    // We only get away with this because MC is single-threaded. Otherwise, this'd have to go into a thread context
    protected double lastMatch;
    protected ITrigger lastSource;

    /**
     * Validate as a manipulator
     * @param need The parent need
     * @throws IllegalArgumentException If anything fails validation
     */
    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        validateCondition(need, this);
    }

    @Override
    public void validateCondition(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        if (parentCondition == null && triggers.isEmpty()) throw new IllegalArgumentException("Root conditions must have at least one trigger.");
        triggers.forEach((c) -> c.validateTrigger(parentNeed, this));
    }

    /**
     * Load as a manipulator
     */
    @Override
    public void onLoaded() {
        super.onLoaded();
        onConditionLoaded(parent, null);
    }

    @Override
    public void onConditionLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parentCondition = parentCondition;
        triggers.forEach(t -> t.onTriggerLoaded(parentNeed, this));
    }

    @Override
    public void onUnloaded() {
        onConditionUnloaded();
    }

    @Override
    public void onConditionUnloaded() {
        triggers.forEach(ITrigger::onTriggerUnloaded);
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

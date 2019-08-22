package org.winterblade.minecraft.mods.needs.manipulators.conditionals;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Represents a series of conditions that will be tested one after another to check that only " +
        "one condition returns true.")
public class XorConditionalManipulator extends OrConditionalManipulator {
    /**
     * Validate as a condition
     * @param parentNeed      The parent need
     * @param parentCondition The parent condition
     */
    @Override
    public void validateCondition(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        if (conditions.size() < 2) throw new IllegalArgumentException("There must be at least two condition.");
        super.validateCondition(parentNeed, parentCondition);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        lastMatch = 0;
        boolean matched = false;
        for (final ICondition condition : conditions) {
            if (condition.test(player)) {
                if (matched) return false;

                if (getMatch) lastMatch = condition.getAmount(player);
                matched = true;
            }
        }

        return matched;
    }
}

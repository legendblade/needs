package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "A trigger to cause the conditional manipulator to check its conditions.")
public interface ITrigger {
    default void validateTrigger(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {}

    void onTriggerLoaded(Need parentNeed, ConditionalManipulator parentCondition);

    void onTriggerUnloaded();

    double getAmount(PlayerEntity player);
}

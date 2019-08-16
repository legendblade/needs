package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

public interface ITrigger {
    default void validateTrigger(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {}

    void onTriggerLoaded(Need parentNeed, ConditionalManipulator parentCondition);

    void onTriggerUnloaded();

    double getAmount(PlayerEntity player);
}

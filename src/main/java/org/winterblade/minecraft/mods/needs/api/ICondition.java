package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.function.Predicate;

@Document(description = "A condition to run in order to verify if the conditional manipulator should complete.")
public interface ICondition extends Predicate<PlayerEntity> {
    void validateCondition(Need parentNeed, ConditionalManipulator parentCondition) throws IllegalArgumentException;

    default void onConditionLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {}

    default void onConditionUnloaded() {}

    double getAmount(PlayerEntity player);
}

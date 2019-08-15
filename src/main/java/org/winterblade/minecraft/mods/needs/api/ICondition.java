package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;

import java.util.function.Predicate;

public interface ICondition extends Predicate<PlayerEntity> {
    void validateCondition(ConditionalManipulator parent) throws IllegalArgumentException;

    default void onConditionLoaded(final ConditionalManipulator parent) {}

    default void onConditionUnloaded() {}

    double getAmount(PlayerEntity player);
}

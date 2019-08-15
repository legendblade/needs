package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;

public interface ITrigger {
    void validateTrigger(ConditionalManipulator parent) throws IllegalArgumentException;

    void onTriggerLoaded(ConditionalManipulator parent);

    void onTriggerUnloaded();

    double getAmount(PlayerEntity player);
}

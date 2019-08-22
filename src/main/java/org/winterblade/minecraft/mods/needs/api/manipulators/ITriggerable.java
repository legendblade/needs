package org.winterblade.minecraft.mods.needs.api.manipulators;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.ITrigger;

/**
 * Interface defining a manipulator that can be triggered via triggers on it or on conditions it contains
 */
public interface ITriggerable {
    void trigger(PlayerEntity player, ITrigger trigger);
}

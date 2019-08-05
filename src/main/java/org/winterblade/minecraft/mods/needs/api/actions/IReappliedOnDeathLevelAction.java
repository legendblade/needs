package org.winterblade.minecraft.mods.needs.api.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

public interface IReappliedOnDeathLevelAction extends ILevelAction {
    /**
     * Called when declared as an entry action and the player respawns
     * @param need      The need calling this
     * @param level     The need level this is part of
     * @param player    The player involved
     * @param oldPlayer The previous player entity (e.g. the corpse)
     */
    void onRespawned(Need need, NeedLevel level, PlayerEntity player, PlayerEntity oldPlayer);

    /**
     * Called when declared as a continuous action and the player respawns
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     * @param oldPlayer The previous player entity (e.g. the corpse)
     */
    void onRespawnedWhenContinuous(Need need, NeedLevel level, PlayerEntity player, PlayerEntity oldPlayer);
}

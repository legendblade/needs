package org.winterblade.minecraft.mods.needs.api.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

public abstract class TickingLevelAction extends LevelAction {
    /**
     * Called every tick per player when declared as a continuous action
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    public void onContinuousTick(final Need need, final NeedLevel level, final PlayerEntity player) {

    }
}

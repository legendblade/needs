package org.winterblade.minecraft.mods.needs.api.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

public abstract class LevelAction implements ILevelAction {

    /**
     * Called when declared as an entry action and the level is entered
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    @Override
    public void onEntered(final Need need, final NeedLevel level, final PlayerEntity player) {

    }

    /**
     * Called when declared as an exit action and the level is exited
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    @Override
    public void onExited(final Need need, final NeedLevel level, final PlayerEntity player) {

    }

    /**
     * Called when declared as a continuous action and the level is entered
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    @Override
    public void onContinuousStart(final Need need, final NeedLevel level, final PlayerEntity player) {

    }

    /**
     * Called when declared as a continuous action and the level is exited
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    @Override
    public void onContinuousEnd(final Need need, final NeedLevel level, final PlayerEntity player) {

    }
}


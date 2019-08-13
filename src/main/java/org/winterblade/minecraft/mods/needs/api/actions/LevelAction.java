package org.winterblade.minecraft.mods.needs.api.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

public abstract class LevelAction implements ILevelAction {
    private Need parentNeed;
    private NeedLevel parentLevel;

    @Override
    public void validate(final Need parentNeed, final NeedLevel parentLevel) throws IllegalArgumentException {

    }

    @Override
    public void onLoaded(final Need parentNeed, final NeedLevel parentLevel) {
        this.parentNeed = parentNeed;
        this.parentLevel = parentLevel;
    }

    @Override
    public void onUnloaded() {
        this.parentNeed = null;
        this.parentLevel = null;
    }

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

    /**
     * Returns true if the need and level provided match the parents of this action
     * @param need  The need to check
     * @param level The level to check
     * @return True if they match, false otherwise.
     */
    protected boolean matches(final Need need, final NeedLevel level) {
        return parentNeed.equals(need) && parentLevel.equals(level);
    }

    /**
     * Returns true if the need provided match the parents of this action
     * @param need  The need to check
     * @return True if they match, false otherwise.
     */
    protected boolean matches(final Need need) {
        return parentNeed.equals(need);
    }

    /**
     * Returns true if the level provided matches the parents of this action
     * @param level The level to check
     * @return True if they match, false otherwise.
     */
    protected boolean matches(final NeedLevel level) {
        return parentLevel.equals(level);
    }
}


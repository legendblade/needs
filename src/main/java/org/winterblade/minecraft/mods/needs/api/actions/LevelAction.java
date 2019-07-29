package org.winterblade.minecraft.mods.needs.api.actions;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;

@JsonAdapter(LevelActionRegistry.class)
public abstract class LevelAction {
    public abstract String getName();

    /**
     * Called when declared as an entry action and the level is entered
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    public void onEntered(Need need, NeedLevel level, PlayerEntity player) {

    }

    /**
     * Called when declared as an exit action and the level is exited
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    public void onExited(Need need, NeedLevel level, PlayerEntity player) {

    }

    /**
     * Called when declared as a continuous action and the level is entered
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    public void onContinuousStart(Need need, NeedLevel level, PlayerEntity player) {

    }

    /**
     * Called when declared as a continuous action and the level is exited
     * @param need   The need calling this
     * @param level  The need level this is part of
     * @param player The player involved
     */
    public void onContinuousEnd(Need need, NeedLevel level, PlayerEntity player) {

    }
}


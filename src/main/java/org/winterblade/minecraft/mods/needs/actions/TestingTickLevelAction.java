package org.winterblade.minecraft.mods.needs.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.actions.TickingLevelAction;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

public class TestingTickLevelAction extends TickingLevelAction {
    @Override
    public String getName() {
        return "Testing";
    }

    @Override
    public void onContinuousTick(Need need, NeedLevel level, PlayerEntity player) {
        NeedsMod.LOGGER.debug(player.getDisplayName().getString() + " ticked at " + player.ticksExisted);
    }
}

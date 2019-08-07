package org.winterblade.minecraft.mods.needs.actions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.actions.TickingLevelAction;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

@Document(description = "This is a debug action; don't use it unless you want your console spammed.")
public class TestingTickLevelAction extends TickingLevelAction {
    @Override
    public String getName() {
        return "Testing";
    }

    @Override
    public void onContinuousTick(final Need need, final NeedLevel level, final PlayerEntity player) {
        NeedsMod.LOGGER.debug(player.getDisplayName().getString() + " ticked at " + player.ticksExisted);
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

public class PerHourManipulator extends BaseManipulator {
    @Expose
    private int amount;

    private long lastFired = 0;

    public PerHourManipulator() {
        amount = -1;
    }

    @Override
    public void onCreated() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent evt) {
        if(evt.world.isRemote) return;

        long worldHour = Math.floorDiv(evt.world.getDayTime(), 1000);

        if(lastFired <= 0) {
            // TODO: This needs serialized
            lastFired = worldHour;
            return;
        }

        // TODO: deal with negative delta because `time set` was used
        long delta = worldHour - lastFired;
        if (delta < 1) return;

        lastFired = worldHour;

        int adjust;
        try {
            adjust = Math.toIntExact(delta * amount);
        } catch (Exception e) {
            NeedsMod.LOGGER.error("Unable to calculate adjustment amount because it's too large.");
            return;
        }

        evt.world.getPlayers().forEach((p) -> parent.adjustValue(p, adjust, this));
    }
}

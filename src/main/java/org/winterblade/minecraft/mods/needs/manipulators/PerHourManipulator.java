package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

public class PerHourManipulator extends BaseManipulator {
    @Expose
    private NeedExpressionContext amount;

    private long lastFired = 0;

    @SubscribeEvent
    public void onTick(final TickEvent.WorldTickEvent evt) {
        if(evt.world.isRemote) return;

        final long worldHour = Math.floorDiv(evt.world.getDayTime(), 1000);

        if(lastFired <= 0) {
            // TODO: This needs serialized
            lastFired = worldHour;
            return;
        }

        // TODO: deal with negative delta because `time set` was used
        final long delta = worldHour - lastFired;
        if (delta < 1) return;

        lastFired = worldHour;

        evt.world.getPlayers().forEach((p) -> {
            final double adjust;
            amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue(p));

            try {
                adjust = delta * amount.get();
            } catch (final Exception e) {
                NeedsMod.LOGGER.error("Unable to calculate adjustment amount because it's too large.");
                return;
            }

            parent.adjustValue(p, adjust, this);
        });
    }
}

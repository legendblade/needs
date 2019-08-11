package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DimensionBasedManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Fired every in-game hour that passes in a world after the first; will multiply its amount " +
        "by the number of hours that have passed if time skips forward. May not work if time is set back.")
public class PerHourManipulator extends DimensionBasedManipulator {
    @Expose
    @Document(description = "The amount to set per hour")
    private NeedExpressionContext amount;

    private long lastFired = 0;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

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
            if (failsDimensionCheck(p)) return;

            final double adjust;
            amount.setCurrentNeedValue(parent, p);

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

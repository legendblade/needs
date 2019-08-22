package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.function.Consumer;

@Document(description = "Fired every in-game hour that passes in a world after the first; will multiply its amount " +
        "by the number of hours that have passed if time skips forward. May not work if time is set back.")
public class PerHourManipulator extends BaseManipulator implements ITrigger {
    @Expose
    @Document(description = "The amount to set per hour")
    private NeedExpressionContext amount;

    private long lastFired = 0;
    private Consumer<PlayerEntity> callback;
    private long lastDelta;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        // Nah?
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        callback = (p) -> parent.adjustValue(p, getAmount(p), this);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        amount.build();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        callback = (p) -> parentCondition.trigger(p, this);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        if (amount != null) amount.build();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerUnloaded() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;

        amount.setCurrentNeedValue(parent, player);
        return lastDelta * amount.apply(player);
    }

    private void onTick(final TickEvent.WorldTickEvent evt) {
        if(evt.world.isRemote) return;

        final long worldHour = Math.floorDiv(evt.world.getDayTime(), 1000);

        if(lastFired <= 0) {
            // TODO: This needs serialized
            lastFired = worldHour;
            return;
        }

        // TODO: deal with negative delta because `time set` was used
        lastDelta = worldHour - lastFired;
        if (lastDelta < 1) return;

        lastFired = worldHour;

        evt.world.getPlayers().forEach(callback);
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

public class SleepingManipulator extends BaseManipulator implements ITrigger {
    @Expose
    @Document(description = "The amount to change by.")
    private NeedExpressionContext amount;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "If this should trigger for successful sleeps")
    @OptionalField(defaultValue = "True")
    private boolean slept = true;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "If this should trigger for the player being woken up without sleeping")
    @OptionalField(defaultValue = "False")
    private boolean woken = false;

    private ITriggerable parentCondition;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        validateCommon();
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        amount.build();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        if (amount != null) amount.build();
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asTrigger);
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
        return amount.apply(player);
    }

    private void asManipulator(final PlayerWakeUpEvent event) {
        if ((!slept || !event.shouldSetSpawn()) && (!woken || event.shouldSetSpawn())) return;
        parent.adjustValue(event.getPlayer(), getAmount(event.getPlayer()), this);
    }

    private void asTrigger(final PlayerWakeUpEvent event) {
        if ((!slept || !event.shouldSetSpawn()) && (!woken || event.shouldSetSpawn())) return;
        parentCondition.trigger(event.getPlayer(), this);
    }

    private void validateCommon() {
        if (!slept && !woken) throw new IllegalArgumentException("Must be checking for at least slept or woken.");
    }
}

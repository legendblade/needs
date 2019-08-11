package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.DimensionBasedManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

public class SleepingManipulator extends DimensionBasedManipulator {
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

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (!slept && !woken) throw new IllegalArgumentException("Must be checking for at least slept or woken.");
        super.validate(need);
    }

    @SubscribeEvent
    protected void onSleep(final PlayerWakeUpEvent event) {
        if (failsDimensionCheck(event.getPlayer())) return;

        if ((!slept || !event.shouldSetSpawn()) && (!woken || event.shouldSetSpawn())) return;

        amount.setCurrentNeedValue(parent, event.getPlayer());
        parent.adjustValue(event.getPlayer(), amount.get(), this);
    }
}

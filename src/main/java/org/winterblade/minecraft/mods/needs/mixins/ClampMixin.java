package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Prevents adjustments to this need from exceeding the specified range. Outside adjustments to " +
        "the need (e.g. health going down due to an attack) will not be affected.\n\n" +
        "Note that if the original adjustment amount is 0, it will never be applied.\n\n" +
        "This will be applied after the `filter` mixin, if present.")
public class ClampMixin extends BaseMixin {
    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The lowest adjustment that can be made to the need")
    @OptionalField(defaultValue = "None")
    private double lowest = Double.NEGATIVE_INFINITY;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "The highest adjustment that can be made to the need")
    @OptionalField(defaultValue = "None")
    private double highest = Double.POSITIVE_INFINITY;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "If the values should be treated as a delta or not.\n" +
            "- When false a lower bound of 5 would prevent the need from ever being adjusted lower than 5 (e.g. -5, 1, " +
            "and 4 would all become 5).\n" +
            "- When false a lower bound of 5 would prevent the need from ever being changed by less than 5 (e.g. 1 and " +
            "4 would become 5, but -5 is fine because it's a change equalling 5 or more)")
    @OptionalField(defaultValue = "False")
    private boolean delta = false;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (lowest == Double.NEGATIVE_INFINITY && highest == Double.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("At least one bound - lowest or highest - should be set.");
        }
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAdjust(final NeedAdjustmentEvent.Pre event) {
        if (!event.getNeed().equals(need)) return;

        double amt = delta ? Math.abs(event.getAmount()) : event.getAmount();

        if (amt < lowest) amt = lowest;
        else if (highest < amt) amt = highest;
        else return; // No change

        event.setAmount(delta && event.getAmount() < 0 ? -amt : amt);
    }
}

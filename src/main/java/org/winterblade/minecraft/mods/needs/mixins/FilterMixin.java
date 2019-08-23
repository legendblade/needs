package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.expressions.FilterExpressionContext;

@Document(description = "Filters any adjustments to the need prior to the adjustment being applied; if the output " +
        "of the function is 0, no adjustment will occur. Outside adjustments to the need (e.g. health going down due " +
        "to an attack) will not be affected.\n\n" +
        "Note that if the original adjustment amount is 0, it will never be applied.\n\n" +
        "This will be applied prior to the `clamp` mixin, if present.")
public class FilterMixin extends BaseMixin {
    @Expose
    @Document(description = "The filter function to apply.")
    private FilterExpressionContext filter;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (filter == null) throw new IllegalArgumentException("Filter function must be specified.");
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        filter.build();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onAdjust(final NeedAdjustmentEvent.Pre event) {
        if (!event.getNeed().equals(need)) return;

        final double amt = event.getAmount();
        filter.setCurrentNeedValue(need, event.getPlayer());
        filter.setIfRequired(FilterExpressionContext.ADJUSTMENT, () -> amt);
        event.setAmount(filter.apply(event.getPlayer()));
    }
}

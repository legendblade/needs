package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class OnNeedChangedManipulator extends BaseManipulator {
    @Expose
    protected String need;

    @Expose
    protected double minChange;

    @Expose
    protected double maxChange;

    @Expose
    protected double minValue;

    @Expose
    protected double maxValue;

    @Expose
    protected OtherNeedChangedExpressionContext amount;

    protected Need otherNeed;

    protected boolean isListening;
    protected boolean checkValue;

    public OnNeedChangedManipulator() {
        minChange = Double.NEGATIVE_INFINITY;
        minValue = Double.NEGATIVE_INFINITY;

        maxChange = Double.POSITIVE_INFINITY;
        maxValue = Double.POSITIVE_INFINITY;
    }

    @Override
    public void onCreated() {
        if (need == null) throw new JsonParseException("onNeedChanged requires a 'need' property.");
        NeedRegistry.INSTANCE.registerDependentNeed(need);
        isListening = true;

        checkValue = (minValue != Double.NEGATIVE_INFINITY || maxValue != Double.POSITIVE_INFINITY);
    }

    @Nullable
    public Need getOtherNeed() {
        if (otherNeed != null) return otherNeed;

        otherNeed = NeedRegistry.INSTANCE.getByName(need);

        if (otherNeed == null && isListening) {
            NeedsMod.LOGGER.error(
                "Unable to get need '" + need + "' for onNeedChanged in '" +
                parent.getName() + "'; updates will not be checked after this."
            );

            MinecraftForge.EVENT_BUS.unregister(this);
            isListening = false;
        }

        return otherNeed;
    }

    @SubscribeEvent
    protected void onOtherNeedChanged(final NeedAdjustmentEvent.Post event) {
        if(!event.getNeed().equals(getOtherNeed())) return;

        final double diff = event.getCurrent() - event.getPrevious();
        if (diff < minChange || maxChange < diff) return;

        if (checkValue) {
            final double value = event.getNeed().getValue(event.getPlayer());
            if (value < minValue || maxValue < value) return;

            amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> value);
        } else {
            amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue(event.getPlayer()));
        }

        amount.setIfRequired("other", event::getCurrent);
        amount.setIfRequired("previous", event::getPrevious);
        amount.setIfRequired("change", () -> diff);

        // TODO: Determine best way to prevent loops
        parent.adjustValue(event.getPlayer(), amount.get(), this);
    }

    @JsonAdapter(ExpressionContext.Deserializer.class)
    public static class OtherNeedChangedExpressionContext extends NeedExpressionContext {
        public OtherNeedChangedExpressionContext() {
        }

        @Override
        protected List<String> getElements() {
            final List<String> elements = super.getElements();
            elements.add("other");
            elements.add("previous");
            elements.add("change");
            return elements;
        }
    }
}

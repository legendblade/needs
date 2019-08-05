package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class OnNeedChangedManipulator extends BaseManipulator {
    @Expose
    protected LazyNeed need;

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
        isListening = true;

        checkValue = (minValue != Double.NEGATIVE_INFINITY || maxValue != Double.POSITIVE_INFINITY);
    }

    @SubscribeEvent
    protected void onOtherNeedChanged(final NeedAdjustmentEvent.Post event) {
        need.get(event, this::onNeedExists, this::onNonexistentNeed);
    }

    protected void onNeedExists(final Need other, final NeedAdjustmentEvent.Post event) {
        if(!event.getNeed().equals(other)) return;

        final double diff = event.getCurrent() - event.getPrevious();
        if (diff < minChange || maxChange < diff) return;

        if (checkValue) {
            final double value = event.getNeed().getValue(event.getPlayer());
            if (value < minValue || maxValue < value) return;

            amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> value);
        } else {
            amount.setCurrentNeedValue(parent, event.getPlayer());
        }

        amount.setIfRequired("other", event::getCurrent);
        amount.setIfRequired("previous", event::getPrevious);
        amount.setIfRequired("change", () -> diff);

        // TODO: Determine best way to prevent loops
        parent.adjustValue(event.getPlayer(), amount.get(), this);
    }

    public void onNonexistentNeed() {
        if (!isListening) return;

        NeedsMod.LOGGER.error(
                "Unable to get need '" + need + "' for onNeedChanged in '" +
                        parent.getName() + "'; updates will not be checked after this."
        );

        MinecraftForge.EVENT_BUS.unregister(this);
        isListening = false;
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

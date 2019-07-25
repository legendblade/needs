package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.util.expressions.NeedExpressionContext;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class OnNeedChangedManipulator extends BaseManipulator {
    @Expose
    protected String need;

    @Expose
    protected int min;

    @Expose
    protected int max;

    @Expose
    protected OtherNeedChangedExpressionContext amount;

    protected Class<? extends Need> otherNeed;

    public OnNeedChangedManipulator() {
        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;
    }

    @Override
    public void onCreated() {
        if (need == null) throw new JsonParseException("onNeedChanged requires a 'need' property.");

        otherNeed = NeedRegistry.INSTANCE.getType(need);

        if (otherNeed == null) throw new JsonParseException(need + " is not a valid need to compare to.");
    }

    @SubscribeEvent
    protected void onOtherNeedChanged(NeedAdjustmentEvent.Post event) {
        if(!otherNeed.isAssignableFrom(event.getNeed().getClass())) return;

        double diff = event.getCurrent() - event.getPrevious();
        if (diff < min || max < diff) return;

        amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue(event.getPlayer()));
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
            List<String> elements = super.getElements();
            elements.add("other");
            elements.add("previous");
            elements.add("change");
            return elements;
        }
    }
}

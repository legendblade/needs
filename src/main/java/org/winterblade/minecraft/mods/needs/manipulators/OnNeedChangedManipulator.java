package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.OtherNeedChangedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@SuppressWarnings("WeakerAccess")
@Document(description = "Affect this need when another need has changed; do not set to the same need this is applied to. " +
        "You have been warned. Can be applied multiple times to the same need with different values.")
public class OnNeedChangedManipulator extends BaseManipulator {
    @Expose
    @Document(description = "The name of the other need to check.")
    protected LazyNeed need;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The minimum amount the other need must have changed by in order to trigger this manipulator")
    protected double minChange;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The maximum amount the other need must have changed by in order to trigger this manipulator")
    protected double maxChange;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The minimum current amount of the other need in order to trigger this manipulator")
    protected double minValue;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "The maximum current amount of the other need in order to trigger this manipulator")
    protected double maxValue;

    @Expose
    @Document(description = "The amount to change by when triggered")
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
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        if (this.need == null) throw new IllegalArgumentException("onNeedChanged requires a 'need' property.");
        if (!this.need.isNot(need)) throw new IllegalArgumentException("onNeedChanged cannot target itself.");
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        isListening = true;
        checkValue = (minValue != Double.NEGATIVE_INFINITY || maxValue != Double.POSITIVE_INFINITY);
    }

    @Override
    public void onUnloaded() {
        isListening = false;
        need.discard();
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

        amount.setIfRequired(OtherNeedChangedExpressionContext.OTHER, event::getCurrent);
        amount.setIfRequired(OtherNeedChangedExpressionContext.PREVIOUS, event::getPrevious);
        amount.setIfRequired(OtherNeedChangedExpressionContext.CHANGE, () -> diff);

        // TODO: Determine best way to prevent loops
        TickManager.INSTANCE.doLater(() -> parent.adjustValue(event.getPlayer(), amount.apply(event.getPlayer()), this));
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

}

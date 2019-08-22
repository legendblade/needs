package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.OtherNeedChangedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "Affect this need when another need has changed; do not set to the same need this is applied to. " +
        "You have been warned. Can be applied multiple times to the same need with different values.")
public class OnNeedChangedManipulator extends BaseManipulator implements ITrigger {
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
    private ITriggerable parentCondition;

    private Supplier<Double> lastValue;
    private double lastCurrent;
    private double lastPrevious;
    private double lastDiff;

    public OnNeedChangedManipulator() {
        minChange = Double.NEGATIVE_INFINITY;
        minValue = Double.NEGATIVE_INFINITY;

        maxChange = Double.POSITIVE_INFINITY;
        maxValue = Double.POSITIVE_INFINITY;
    }

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        validateCommon(need);
        super.validate(need);
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon(parentNeed);
    }

    protected void validateCommon(final Need need) {
        if (this.need == null) throw new IllegalArgumentException("onNeedChanged requires a 'need' property.");
        if (!this.need.isNot(need)) throw new IllegalArgumentException("onNeedChanged cannot target itself.");
    }

    @Override
    public void onLoaded() {
        onLoadedCommon();
        super.onLoaded();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
    }

    @Override
    public void onUnloaded() {
        onTriggerUnloaded();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asTrigger);
    }

    @Override
    public void onTriggerUnloaded() {
        isListening = false;
        need.discard();
    }

    protected void onLoadedCommon() {
        if (amount != null) amount.build();
        isListening = true;
        checkValue = (minValue != Double.NEGATIVE_INFINITY || maxValue != Double.POSITIVE_INFINITY);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;

        amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, lastValue);
        amount.setIfRequired(OtherNeedChangedExpressionContext.OTHER, () -> lastCurrent);
        amount.setIfRequired(OtherNeedChangedExpressionContext.PREVIOUS, () -> lastPrevious);
        amount.setIfRequired(OtherNeedChangedExpressionContext.CHANGE, () -> lastDiff);

        return amount.apply(player);
    }

    protected void asManipulator(final NeedAdjustmentEvent.Post event) {
        need.get(event,
                (other, evt) -> onNeedExists(other, evt,
                        (pl) -> parent.adjustValue(pl, getAmount(pl), this)),
                this::onNonexistentNeed);
    }

    protected void asTrigger(final NeedAdjustmentEvent.Post event) {
        need.get(event,
                (other, evt) -> onNeedExists(other, evt,
                        (pl) -> parentCondition.trigger(pl, this)),
                this::onNonexistentNeed);
    }

    protected void onNeedExists(final Need other, final NeedAdjustmentEvent.Post event, final Consumer<PlayerEntity> callback) {
        if(!event.getNeed().equals(other)) return;

        // TODO: Determine best way to prevent loops
        TickManager.INSTANCE.doLater(() -> {
            final double diff = event.getCurrent() - event.getPrevious();
            if (diff < minChange || maxChange < diff) return;

            if (checkValue) {
                final double value = event.getNeed().getValue(event.getPlayer());
                if (value < minValue || maxValue < value) return;

                lastValue = () -> value;
            } else {
                lastValue = () -> parent.getValue(event.getPlayer());
            }

            lastCurrent = event.getCurrent();
            lastPrevious = event.getPrevious();
            lastDiff = diff;

            callback.accept(event.getPlayer());
        });
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

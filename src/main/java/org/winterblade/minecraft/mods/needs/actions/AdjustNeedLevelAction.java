package org.winterblade.minecraft.mods.needs.actions;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;
import org.winterblade.minecraft.mods.needs.capabilities.customneed.INeedCapability;

@SuppressWarnings("WeakerAccess")
@Document(description = "Adjusts the value of either the parent need, or the target need")
public class AdjustNeedLevelAction extends LevelAction {
    static {
        CAPABILITY = null;
    }

    @CapabilityInject(INeedCapability.class)
    public static final Capability<INeedCapability> CAPABILITY;

    @Expose
    @OptionalField(defaultValue = "The parent need")
    @Document(description = "The need to affect; leave out to specify the parent need.")
    protected LazyNeed need;

    @Expose
    @Document(description = "The amount by which to change the targeted need when this action is fired; as a continuous " +
            "action, it will store the amount it changes the need by, and restore it upon exiting the level.")
    protected NeedExpressionContext amount;

    @Override
    public String getName() {
        return "Adjust Need";
    }

    @Override
    public void onLoaded(final Need parentNeed, final NeedLevel parentLevel) {
        super.onLoaded(parentNeed, parentLevel);

        // If there wasn't a need specified, assume they want the parent need.
        if (need == null) need = LazyNeed.of(parentNeed);
    }

    @Override
    public void onEntered(final Need need, final NeedLevel level, final PlayerEntity player) {
        this.need.get(player, this::adjust, this::onError);
    }

    @Override
    public void onExited(final Need need, final NeedLevel level, final PlayerEntity player) {
        this.need.get(player, this::adjust, this::onError);
    }

    @Override
    public void onContinuousStart(final Need need, final NeedLevel level, final PlayerEntity player) {
        this.need.get(player, (n, p) -> {
            final double adjustment = adjust(n, p);

            if (adjustment == 0d) return;
            player.getCapability(CAPABILITY).ifPresent((c) -> c.storeLevelAdjustment(need.getName(), level.getName(), adjustment));
        }, this::onError);
    }

    @Override
    public void onContinuousEnd(final Need need, final NeedLevel level, final PlayerEntity player) {
        this.need.get((o) -> {
            if (player.world.isRemote) return;

            final double amount = player
                    .getCapability(CAPABILITY)
                        .map((c) -> c.getLevelAdjustment(need.getName(), level.getName()))
                        .orElse(0d);

            if (amount == 0d) return;
            TickManager.INSTANCE.doLater(() -> o.adjustValue(player, 0 - amount, BaseManipulator.EXTERNAL));
        }, this::onError);
    }

    protected double adjust(final Need other, final PlayerEntity player) {
        if (player.world.isRemote) return 0;

        if (other instanceof ReadOnlyNeed) {
            NeedsMod.LOGGER.warn("You cannot adjust " + need + ", because it is read-only.");
            return 0;
        }
        amount.setCurrentNeedValue(other, player);

        final double output = amount.apply(player);
        TickManager.INSTANCE.doLater(() -> other.adjustValue(player, output, BaseManipulator.EXTERNAL));

        return output;
    }

    private void onError() {
        NeedsMod.LOGGER.warn("Invalid need " + need + " for need adjustment level action.");
    }
}

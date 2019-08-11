package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.expressions.CountedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.TooltipManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

@Document(description = "Triggered every 5 ticks while the player is holding the specified item")
public class HoldingManipulator extends TooltipManipulator {
    @Expose
    @Document(description = "The amount to change; count refers to the current stack size of the item(s)")
    protected CountedExpressionContext amount;

    @Expose
    @OptionalField(defaultValue = "True")
    @Document(description = "If true, check the player's mainhand for the item.")
    protected boolean mainhand = true;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, check the player's offhand for the item")
    protected boolean offhand = false;

    @Expose
    @JsonAdapter(IIngredient.ToListDeserializer.class)
    @Document(type = IIngredient.class, description = "An array of items to check against, specified either as a single " +
            "item (e.g. 'minecraft:cobblestone') or a tag (e.g. 'tag:minecraft:leaves')")
    protected List<IIngredient> items;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        if (!mainhand && !offhand) throw new IllegalArgumentException("Holding manipulator should set at least one of 'mainhand' or 'offhand'.");

        super.validate(need);
    }

    @Override
    public void onLoaded() {
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::onTick);
        super.onLoaded();

        final String amountType = amount.isConstant() || !amount.isRequired(CountedExpressionContext.COUNT) ? "" : ", Per Item";
        final String suffix;
        if (mainhand && offhand) {
            suffix = "  (In Either Hand" + amountType + ")";
        } else if (offhand) {
            suffix = "  (In Offhand" + amountType + ")";
        } else {
            suffix = "  (In Mainhand" + amountType + ")";
        }

        postFormat = (sb, player) -> sb.append(suffix).toString();
        amount.syncAll();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    @Nullable
    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        amount.setIfRequired(CountedExpressionContext.COUNT, () -> 1d);
        return amount;
    }

    /**
     * Called when ticking to calculate
     * @param player The player
     */
    private void onTick(@Nonnull final PlayerEntity player) {
        if (failsDimensionCheck(player)) return;
        Function<IIngredient, Double> test;

        // TODO: This can be optimized
        final ItemStack mainhand = this.mainhand ? player.getHeldItemMainhand() : ItemStack.EMPTY;
        final ItemStack offhand = this.offhand ? player.getHeldItemOffhand() : ItemStack.EMPTY;

        if (amount.isConstant()) {
            if (items.stream().noneMatch((p) -> p.test(mainhand) || p.test(offhand))) return;
        } else {
            int mainCount = 0;
            int offCount = 0;

            for (final IIngredient item : items) {
                if (mainCount == 0 && item.test(mainhand)) {
                    mainCount = mainhand.getCount();
                }
                if (offCount == 0 && item.test(offhand)) {
                    offCount = offhand.getCount();
                }

                if (mainCount != 0 && offCount != 0) break;
            }

            final double c = mainCount + offCount;
            if (c <= 0) return;
            amount.setIfRequired(CountedExpressionContext.COUNT, () -> c);
        }

        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.apply(player), this);
    }
}

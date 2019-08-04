package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.expressions.CountedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class HoldingManipulator extends TooltipManipulator {
    @Expose
    protected CountedExpressionContext amount;

    @Expose
    protected boolean mainhand = true;

    @Expose
    protected boolean offhand = false;

    @Expose
    @JsonAdapter(IIngredient.ToListDeserializer.class)
    protected List<IIngredient> items;

    @Override
    public void onCreated() {
        if (!mainhand && !offhand) throw new JsonParseException("Holding manipulator should set at least one of 'mainhand' or 'offhand'.");

        TickManager.INSTANCE.requestPlayerTickUpdate(this::onTick);
        super.onCreated();

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

        parent.adjustValue(player, amount.get(), this);
    }
}

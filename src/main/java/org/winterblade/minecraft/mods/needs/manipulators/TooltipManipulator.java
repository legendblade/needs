package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.DistExecutor;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public abstract class TooltipManipulator extends BaseManipulator {
    @Expose
    protected boolean showTooltip;

    @SuppressWarnings("FieldMayBeFinal")
    @Expose
    protected int precision = 1;

    @Expose
    protected String tooltip;

    protected final RangeMap<Double, String> formatting = TreeRangeMap.create();

    protected int capacity = 0;
    protected String precisionFormat;
    protected BiFunction<StringBuilder, PlayerEntity, String> postFormat;

    private static final Supplier<String> emptySupplier = () -> "";

    private final WeakHashMap<ItemStack, Supplier<String>> itemCache = new WeakHashMap<>();
    private LocalCachedNeed localCachedNeed;

    @Override
    public void onCreated() {
        if (showTooltip) {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onTooltip));
        }

        // Rough string capacity for tooltips:
        capacity = parent.getName().length() + 12;
        precisionFormat = "%." + precision + "f";
    }

    /**
     * Called on the client when Minecraft wants to render the tooltip
     * @param event The tooltip event
     */
    protected void onTooltip(final ItemTooltipEvent event) {
        // TODO: Consider better way of caching values:
        final String msg;
        if (itemCache.containsKey(event.getItemStack())) {
            msg = itemCache.get(event.getItemStack()).get();
        } else {
            final ExpressionContext valueExpr = getItemTooltipExpression(event.getItemStack());

            if (valueExpr == null) {
                itemCache.put(event.getItemStack(), emptySupplier);
                return;
            }


            // If we need to update and recalculate continuously because the need might have changed:
            final Supplier<String> msgGetter = () -> {
                setupExpression(() -> getLocalCachedNeed().getValue(), event.getEntityPlayer(), event.getItemStack(), valueExpr);
                return formatOutput(valueExpr.get(), event.getEntityPlayer());
            };
            itemCache.put(event.getItemStack(), msgGetter);

            msg = msgGetter.get();
        }

        final List<ITextComponent> toolTip = event.getToolTip();
        if (msg.isEmpty() || isThisValuePresent(toolTip, msg)) return;
        toolTip.add(new StringTextComponent(msg));
    }

    /**
     * Gets the expression for the given item stack or null if it doesn't match
     * @param item The item stack to test
     * @return The expression, or null if none exists
     */
    @Nullable
    protected abstract ExpressionContext getItemTooltipExpression(ItemStack item);

    /**
     * Sets up the expression
     * @param player The player
     * @param item   The item
     * @param expr   The expression
     */
    protected void setupExpression(final Supplier<Double> currentValue, final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        expr.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, currentValue);
    }

    /**
     * Determines if we've already added info about this need to the tooltip
     * @param tooltip The tooltip
     * @param msg   The msg
     * @return True if we have, false otherwise
     */
    private static boolean isThisValuePresent(final List<ITextComponent> tooltip, final String msg) {
        if (tooltip.size() <= 1) return false;

        for (final ITextComponent line : tooltip) {
            if (line.getString().equals(msg)) return true;
        }
        return false;
    }

    /**
     * Format the output
     * @param value The value to format
     * @param player The player to check
     * @return The formatted string
     */
    private String formatOutput(final double value, final PlayerEntity player) {
        final StringBuilder theLine = new StringBuilder(capacity);
        final String color = formatting.get(value);
        theLine.append(color != null ? color : TextFormatting.AQUA.toString());

        theLine.append(parent.getName());
        theLine.append(": ");

        if (tooltip != null && !tooltip.isEmpty()) return theLine.append(tooltip).toString();

        theLine.append(value < 0 ? '-' : '+');
        theLine.append(String.format(precisionFormat, Math.abs(value)));
        theLine.append("  ");
        theLine.append(value < 0 ? '\u25bc' : '\u25b2'); // Up or down arrows

        return postFormat != null ? postFormat.apply(theLine, player) : theLine.toString();
    }

    /**
     * Gets the local cached need on the client
     * @return The local cached need
     */
    private LocalCachedNeed getLocalCachedNeed() {
        if (localCachedNeed != null) return localCachedNeed;

        localCachedNeed = NeedRegistry.INSTANCE.getLocalCache().get(parent.getName());
        return localCachedNeed;
    }
}

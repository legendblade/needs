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
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public abstract class TooltipManipulator extends BaseManipulator {
    @Expose
    protected boolean showTooltip;

    @SuppressWarnings("FieldMayBeFinal")
    @Expose
    protected int precision = 1;

    protected final RangeMap<Double, String> formatting = TreeRangeMap.create();
    protected int capacity = 0;
    protected String precisionFormat;
    protected Function<StringBuilder, String> postFormat;

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
            ExpressionContext valueExpr = null;
            boolean found = false;
            for (final Map.Entry<IIngredient, ExpressionContext> item : itemValues.entrySet()) {
                if (!item.getKey().test(event.getItemStack())) continue;

                valueExpr = item.getValue();
                setupExpression(event.getEntityPlayer(), event.getItemStack(), valueExpr);

                found = true;
                break;
            }

            if (!found) {
                itemCache.put(event.getItemStack(), emptySupplier);
                return;
            }

            // If we need to update and recalculate continuously because the need might have changed:
            if (valueExpr.isRequired(NeedExpressionContext.CURRENT_NEED_VALUE)) {
                final ExpressionContext expr = valueExpr;
                final Supplier<String> msgGetter = () -> {
                    expr.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> getLocalCachedNeed().getValue());
                    return formatOutput(expr.get());
                };

                itemCache.put(event.getItemStack(), msgGetter);
                msg = msgGetter.get();
            } else {
                msg = formatOutput(valueExpr.get());
                itemCache.put(event.getItemStack(), () -> msg);
            }
        }

        final List<ITextComponent> toolTip = event.getToolTip();
        if (msg.isEmpty() || isThisValuePresent(toolTip, msg)) return;
        toolTip.add(new StringTextComponent(msg));
    }

    protected abstract void setupExpression(final PlayerEntity player, final ItemStack item, final ExpressionContext expr);

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
     * @return The formatted string
     */
    private String formatOutput(final double value) {
        final StringBuilder theLine = new StringBuilder(capacity);
        final String color = formatting.get(value);
        theLine.append(color != null ? color : TextFormatting.AQUA.toString());

        theLine.append(parent.getName());
        theLine.append(": ");
        theLine.append(value < 0 ? '-' : '+');
        theLine.append(String.format(precisionFormat, Math.abs(value)));
        theLine.append("  ");
        theLine.append(value < 0 ? '\u25bc' : '\u25b2'); // Up or down arrows

        return postFormat != null ? postFormat.apply(theLine) : theLine.toString();
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

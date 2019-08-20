package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.DistExecutor;
import org.winterblade.minecraft.mods.needs.api.Formatting;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "A collection of manipulators that can display their effect in the tooltip when hovering " +
        "over the item in an inventory")
public abstract class TooltipManipulator extends BaseManipulator {
    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, show the tooltip. Note that doing so will incur a slight performance penalty on" +
            "the client; defaults to false")
    protected boolean showTooltip;

    @Expose
    @OptionalField(defaultValue = "Normal tooltip")
    @Document(description = "If this is set, will override the value of the tooltip itself with this string.")
    protected String tooltip;

    @Expose
    @Document(description = "A key/value map of formatting/color codes to their associated interval to format tooltips with", type = String.class)
    @JsonAdapter(FormattingDeserializer.class)
    protected final RangeMap<Double, String> formatCode = TreeRangeMap.create();

    @Expose
    @Document(description = "Defines any extra formatting parameters to apply")
    @OptionalField(defaultValue = "None")
    protected Formatting formatting;

    protected int capacity = 0;
    protected BiFunction<StringBuilder, PlayerEntity, String> postFormat;

    private static final Supplier<String> emptySupplier = () -> "";

    private final WeakHashMap<ItemStack, Supplier<String>> itemCache = new WeakHashMap<>();
    private LocalCachedNeed localCachedNeed;

    @Override
    public void onLoaded() {
        if (showTooltip) {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onTooltip));
        }

        // Rough string capacity for tooltips:
        capacity = parent.getName().length() + 12;

        // Default colors:
        if (formatCode.asMapOfRanges().isEmpty()) {
            formatCode.put(Range.greaterThan(0d), TextFormatting.GREEN.toString());
            formatCode.put(Range.lessThan(0d), TextFormatting.RED.toString());
        }

        if (formatting == null) formatting = new Formatting();
        formatting.init();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        MinecraftForge.EVENT_BUS.unregister(this);
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
                setupExpression(() -> getLocalCachedNeed().getValue(), event.getPlayer(), event.getItemStack(), valueExpr);
                return formatOutput(valueExpr.apply(event.getPlayer()), event.getEntityPlayer());
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
        final String color = formatCode.get(value);
        theLine.append(color != null ? color : TextFormatting.AQUA.toString());

        theLine.append(parent.getName());
        theLine.append(": ");

        if (tooltip != null && !tooltip.isEmpty()) return theLine.append(tooltip).toString();

        final double adjustedValue = formatting.calculate(value, player);

        theLine.append(adjustedValue < 0 ? '-' : '+');
        theLine.append(formatting.format(Math.abs(adjustedValue)));
        theLine.append("  ");
        theLine.append(adjustedValue < 0 ? '\u25bc' : '\u25b2'); // Up or down arrows

        return postFormat != null ? postFormat.apply(theLine, player) : theLine.toString();
    }

    /**
     * Gets the local cached need on the client
     * @return The local cached need
     */
    private LocalCachedNeed getLocalCachedNeed() {
        if (localCachedNeed != null) return localCachedNeed;

        localCachedNeed = NeedRegistry.INSTANCE.getLocalNeed(parent.getName());
        return localCachedNeed;
    }

    private static class FormattingDeserializer implements JsonDeserializer<RangeMap<Double, String>> {
        @Override
        public RangeMap<Double, String> deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final TreeRangeMap<Double, String> output = TreeRangeMap.create();
            if (!json.isJsonObject()) return output;

            json.getAsJsonObject().entrySet().forEach((kv) -> {
                final Range<Double> range;
                if (kv.getValue().isJsonPrimitive()) {
                    final String rangeStr = kv.getValue().getAsJsonPrimitive().getAsString();
                    range = RangeHelper.parseStringAsRange(rangeStr);
                } else if (kv.getValue().isJsonObject()) {
                    range = RangeHelper.parseObjectToRange(kv.getValue().getAsJsonObject());
                } else {
                    throw new JsonParseException("Format range must be an object with min/max or a string.");
                }

                final TextFormatting format = TextFormatting.getValueByName(kv.getKey());
                if (format == null) throw new JsonParseException("Unknown format color: " + kv.getKey());
                output.put(range, format.toString());
            });

            return output;
        }
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;
import org.winterblade.minecraft.mods.needs.api.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;
import org.winterblade.minecraft.mods.needs.util.items.ItemIngredient;
import org.winterblade.minecraft.mods.needs.util.items.TagIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends BaseManipulator {
    private static final Supplier<String> emptySupplier = () -> "";

    @Expose
    private FoodExpressionContext defaultAmount;

    @Expose
    private boolean showTooltip;

    @SuppressWarnings("FieldMayBeFinal")
    @Expose
    private int precision = 1;

    private int capacity = 0;

    private final Map<IIngredient, FoodExpressionContext> itemValues = new HashMap<>();

    private final WeakHashMap<ItemStack, Supplier<String>> itemCache = new WeakHashMap<>();
    private final RangeMap<Double, String> formatting = TreeRangeMap.create();

    private LocalCachedNeed localCachedNeed;
    private String precisionFormat;

    @Override
    public void onCreated() {
        if (showTooltip) {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onTooltip));
        }

        // Rough string capacity for tooltips:
        capacity = parent.getName().length() + 12;
        precisionFormat = "%." + precision + "f";
    }

    @SubscribeEvent
    public void OnItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();

        for (final Map.Entry<IIngredient, FoodExpressionContext> entry : itemValues.entrySet()) {
            final ItemStack item = evt.getItem();
            if (!entry.getKey().isMatch(item)) continue;

            final FoodExpressionContext expr = entry.getValue();
           setupFoodExpression(player, item, expr);

            parent.adjustValue(player, expr.get(), this);
            return;
        }
    }

    /**
     * Sets up the food expression
     * @param player The player
     * @param item   The item
     * @param expr   The expression
     */
    protected void setupFoodExpression(final PlayerEntity player, final ItemStack item, final FoodExpressionContext expr) {
        expr.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue(player));

        if (item.isFood()) {
            final Food food = item.getItem().getFood();
            if (food != null) {
                expr.setIfRequired("hunger", () -> (double) food.getHealing());
                expr.setIfRequired("saturation", () -> (double) food.getSaturation());
            }
        }
    }

    /**
     * Called on the client when Minecraft wants to render the tooltip
     * @param event The tooltip event
     */
    private void onTooltip(final ItemTooltipEvent event) {
        // TODO: Consider better way of caching values:
        final String msg;
        if (itemCache.containsKey(event.getItemStack())) {
            msg = itemCache.get(event.getItemStack()).get();
        } else {
            FoodExpressionContext valueExpr = null;
            boolean found = false;
            for (final Map.Entry<IIngredient, FoodExpressionContext> item : itemValues.entrySet()) {
                if (!item.getKey().isMatch(event.getItemStack())) continue;

                valueExpr = item.getValue();
                setupFoodExpression(event.getEntityPlayer(), event.getItemStack(), valueExpr);

                found = true;
                break;
            }

            if (!found) {
                itemCache.put(event.getItemStack(), emptySupplier);
                return;
            }

            // If we need to update and recalculate continuously because the need might have changed:
            if (valueExpr.isRequired(NeedExpressionContext.CURRENT_NEED_VALUE)) {
                final FoodExpressionContext expr = valueExpr;
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
        if (msg.isEmpty() || isThisValuePresent(toolTip)) return;
        toolTip.add(new StringTextComponent(msg));
    }

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

        return theLine.toString();
    }

    /**
     * Determines if we've already added info about this need to the tooltip
     * @param tooltip The tooltip
     * @return True if we have, false otherwise
     */
    private boolean isThisValuePresent(final List<ITextComponent> tooltip) {
        if (tooltip.size() <= 1) return false;

        for (final ITextComponent line : tooltip) {
            final String lineString = line.getString();
            final String withoutFormatting = TextFormatting.getTextWithoutFormattingCodes(lineString);
            if (withoutFormatting == null) continue;

            if (withoutFormatting.startsWith(parent.getName())) return true;
        }
        return false;
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

    class Deserializer implements JsonDeserializer<ItemUsedManipulator> {
        @Override
        public ItemUsedManipulator deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) throw new JsonParseException("ItemUsed must be an object");

            // TODO: Get gson to heavy-lift the base class
            final ItemUsedManipulator output = new ItemUsedManipulator();

            final JsonObject obj = json.getAsJsonObject();
            if (!obj.has("items")) throw new JsonParseException("ItemUsed must have an items array");

            if (obj.has("defaultAmount")) output.defaultAmount = context.deserialize(obj.get("defaultAmount"), FoodExpressionContext.class);
            else output.defaultAmount = ExpressionContext.makeConstant(new FoodExpressionContext(),1);

            if (obj.has("showTooltip")) output.showTooltip = obj.get("showTooltip").getAsBoolean();

            // Get formatting ranges:
            if (obj.has("formatting")) {
                final JsonObject formattingEl = obj.getAsJsonObject("formatting");
                formattingEl.entrySet().forEach((kv) -> {
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
                    output.formatting.put(range, format.toString());
                });
            } else {
                output.formatting.put(Range.greaterThan(0d), TextFormatting.GREEN.toString());
                output.formatting.put(Range.lessThan(0d), TextFormatting.RED.toString());
            }

            // Now actually deal with the items:
            final JsonElement itemsEl = obj.get("items");

            // Handle if it's a key/value pair:
            if (itemsEl.isJsonObject()) {
                final JsonObject itemsObj = itemsEl.getAsJsonObject();
                itemsObj.entrySet().forEach((pair) -> {
                    final IIngredient ingredient = getIngredient(pair.getKey());
                    if(ingredient == null) return;

                    final JsonElement value = pair.getValue();
                    output.itemValues.put(ingredient, !value.isJsonNull()
                            ? context.deserialize(value, FoodExpressionContext.class)
                            : output.defaultAmount);
                });
                return output;
            }

            if (!itemsEl.isJsonArray()) throw new JsonParseException("ItemUsed must have an items array or object");

            // Handle if it's an array:
            final JsonArray itemArray = obj.getAsJsonArray("items");
            if (itemArray == null || itemArray.size() <= 0) throw new JsonParseException("ItemUsed must have an items array");

            itemArray.forEach((el) -> {
                FoodExpressionContext amount = output.defaultAmount;
                final String entry;
                IIngredient ingredient = null;

                if(el.isJsonPrimitive()) {
                    entry = el.getAsString();
                    ingredient = getIngredient(entry);
                } else if(el.isJsonObject()) {
                    final JsonObject elObj = el.getAsJsonObject();

                    if (elObj.has("predicate")) {
                        ingredient = getIngredient(elObj.getAsJsonPrimitive("predicate").getAsString());
                    }

                    if (elObj.has("amount")) {
                        amount = context.deserialize(obj.get("defaultAmount"), FoodExpressionContext.class);
                    }
                } else {
                    throw new JsonParseException("Unknown item format: " + el.toString());
                }

                if(ingredient == null) return;

                output.itemValues.put(ingredient, amount);
            });

            return output;
        }

        /**
         * Gets an ingredient from a string representation
         * @param input     The string to parse
         * @return          The parsed ingredient, or null if parsing failed.
         */
        private IIngredient getIngredient(final String input) {
            if(input == null || input.isEmpty()) return null;

            if (input.startsWith("tag:")) {
                final Tag<Item> tag = ItemTags.getCollection().get(new ResourceLocation(input.substring(4)));

                if (tag == null) {
                    NeedsMod.LOGGER.warn("Unknown tag " + input);
                    return null;
                }

                return new TagIngredient(tag);
            }

            final Item item = RegistryManager.ACTIVE.getRegistry(Item.class).getValue(new ResourceLocation(input));
            if (item == null) {
                NeedsMod.LOGGER.warn("Unknown item " + input);
                return null;
            }

            return new ItemIngredient(new ItemStack(item));
        }
    }

    @JsonAdapter(ExpressionContext.Deserializer.class)
    public static class FoodExpressionContext extends NeedExpressionContext {
        public FoodExpressionContext() {
        }

        @Override
        protected List<String> getElements() {
            final List<String> elements = super.getElements();
            elements.add("hunger");
            elements.add("saturation");
            return elements;
        }
    }
}

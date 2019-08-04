package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.Range;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;
import org.winterblade.minecraft.mods.needs.util.items.ItemIngredient;
import org.winterblade.minecraft.mods.needs.util.items.TagIngredient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends TooltipManipulator {
    @Expose
    private FoodExpressionContext defaultAmount;

    public ItemUsedManipulator() {
        postFormat = (sb) -> sb.append("  (Use)").toString();
    }

    @SubscribeEvent
    public void OnItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();

        for (final Map.Entry<IIngredient, ExpressionContext> entry : itemValues.entrySet()) {
            final ItemStack item = evt.getItem();
            if (!entry.getKey().test(item)) continue;

            final ExpressionContext expr = entry.getValue();
           setupExpression(player, item, expr);

            parent.adjustValue(player, expr.get(), this);
            return;
        }
    }

    /**
     * Sets up the expression
     * @param player The player
     * @param item   The item
     * @param expr   The expression
     */
    protected void setupExpression(final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        expr.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue(player));

        if (item.isFood()) {
            final Food food = item.getItem().getFood();
            if (food != null) {
                expr.setIfRequired("hunger", () -> (double) food.getHealing());
                expr.setIfRequired("saturation", () -> (double) food.getSaturation());
            }
        }
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
                return new TagIngredient(new ResourceLocation(input.substring(4)));
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

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.Range;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends TooltipManipulator {
    @Expose
    private FoodExpressionContext defaultAmount;

    protected final Map<Predicate<ItemStack>, ExpressionContext> itemValues = new HashMap<>();

    public ItemUsedManipulator() {
        postFormat = (sb, player) -> sb.append("  (Use)").toString();
    }

    @SubscribeEvent
    public void OnItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();

        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : itemValues.entrySet()) {
            final ItemStack item = evt.getItem();
            if (!entry.getKey().test(item)) continue;

            final ExpressionContext expr = entry.getValue();
            setupExpression(() -> parent.getValue(player), player, item, expr);

            parent.adjustValue(player, expr.get(), this);
            return;
        }
    }

    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : itemValues.entrySet()) {
            if (entry.getKey().test(item)) return entry.getValue();
        }
        return null;
    }

    /**
     * Sets up the expression
     * @param player The player
     * @param item   The item
     * @param expr   The expression
     */
    protected void setupExpression(final Supplier<Double> currentValue, final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        super.setupExpression(currentValue, player, item, expr);

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
                    final IIngredient ingredient = IIngredient.getIngredient(pair.getKey());
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
                IIngredient ingredient = null;

                if(el.isJsonPrimitive()) {
                    ingredient = context.deserialize(el, IIngredient.class);
                } else if(el.isJsonObject()) {
                    final JsonObject elObj = el.getAsJsonObject();

                    if (elObj.has("predicate")) {
                        ingredient = context.deserialize(elObj.getAsJsonPrimitive("predicate"), IIngredient.class);
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

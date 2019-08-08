package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "Triggered when the player uses an item in the list")
public class ItemUsedManipulator extends TooltipManipulator {
    @Expose
    @Document(description = "The default amount to apply if not specified on an item level")
    private FoodExpressionContext defaultAmount;

    @Expose
    @Document(description = "An key/value map of items/amounts, or array of items using the default amount")
    @JsonAdapter(ItemValueDeserializer.class)
    protected final Map<Predicate<ItemStack>, ExpressionContext> items = new HashMap<>();

    public ItemUsedManipulator() {
        postFormat = (sb, player) -> sb.append("  (Use)").toString();
    }

    @Override
    public void onCreated() {
        super.onCreated();
        items.entrySet().forEach((kv) -> {
            if (kv.getValue() == null) kv.setValue(defaultAmount);
        });
    }

    @SubscribeEvent
    public void onItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();

        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : items.entrySet()) {
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
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : items.entrySet()) {
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
                expr.setIfRequired(FoodExpressionContext.HUNGER, () -> (double) food.getHealing());
                expr.setIfRequired(FoodExpressionContext.SATURATION, () -> (double) food.getSaturation());
            } else {
                expr.setIfRequired(FoodExpressionContext.HUNGER, () -> 0d);
                expr.setIfRequired(FoodExpressionContext.SATURATION, () -> 0d);
            }
        } else {
            expr.setIfRequired(FoodExpressionContext.HUNGER, () -> 0d);
            expr.setIfRequired(FoodExpressionContext.SATURATION, () -> 0d);
        }
    }

    @JsonAdapter(ExpressionContext.Deserializer.class)
    public static class FoodExpressionContext extends NeedExpressionContext {
        public static final String HUNGER = "hunger";
        public static final String SATURATION = "saturation";

        protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);
        static {
            docs.put(HUNGER, "The amount of pips on the hunger bar this refills, if food.");
            docs.put(SATURATION, "The amount of saturation this grants, if food.");
        }

        public FoodExpressionContext() {
        }

        @Override
        public List<String> getElements() {
            final List<String> elements = super.getElements();
            elements.add(HUNGER);
            elements.add(SATURATION);
            return elements;
        }

        @Override
        public Map<String, String> getElementDocumentation() {
            return docs;
        }
    }

    private static class ItemValueDeserializer implements JsonDeserializer<Map<Predicate<ItemStack>, ExpressionContext>> {

        @Override
        public Map<Predicate<ItemStack>, ExpressionContext> deserialize(final JsonElement itemsEl, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final Map<Predicate<ItemStack>, ExpressionContext> output = new HashMap<>();
            // Handle if it's a key/value pair:
            if (itemsEl.isJsonObject()) {
                final JsonObject itemsObj = itemsEl.getAsJsonObject();
                itemsObj.entrySet().forEach((pair) -> {
                    final IIngredient ingredient = IIngredient.getIngredient(pair.getKey());
                    if(ingredient == null) return;

                    final JsonElement value = pair.getValue();
                    output.put(ingredient, !value.isJsonNull()
                            ? context.deserialize(value, FoodExpressionContext.class)
                            : null);
                });
                return output;
            }

            if (!itemsEl.isJsonArray()) throw new JsonParseException("ItemUsed must have an items array or object");

            // Handle if it's an array:
            final JsonArray itemArray = itemsEl.getAsJsonArray();
            if (itemArray == null || itemArray.size() <= 0) throw new JsonParseException("ItemUsed must have an items array");

            itemArray.forEach((el) -> {
                FoodExpressionContext amount = null;
                IIngredient ingredient = null;

                if(el.isJsonPrimitive()) {
                    ingredient = context.deserialize(el, IIngredient.class);
                } else if(el.isJsonObject()) {
                    final JsonObject elObj = el.getAsJsonObject();

                    if (elObj.has("predicate")) {
                        ingredient = context.deserialize(elObj.getAsJsonPrimitive("predicate"), IIngredient.class);
                    }

                    if (elObj.has("amount")) {
                        amount = context.deserialize(elObj.get("amount"), FoodExpressionContext.class);
                    }
                } else {
                    throw new JsonParseException("Unknown item format: " + el.toString());
                }

                if(ingredient == null) return;

                output.put(ingredient, amount);
            });

            return output;
        }
    }
}

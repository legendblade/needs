package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.TooltipManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
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
    public void validate(final Need need) throws IllegalArgumentException {
        //noinspection ConstantConditions - never, ever trust user input.
        if (getItems() == null || getItems().isEmpty()) throw new IllegalArgumentException("Items array must be specified.");

        if (getDefaultAmount() == null && getItems().entrySet().stream().anyMatch((kv) -> kv.getValue() == null)) {
            throw new IllegalArgumentException("Default amount must be specified if one or more items use it.");
        }

        super.validate(need);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        getItems().entrySet().forEach((kv) -> {
            if (kv.getValue() == null) kv.setValue(getDefaultAmount());
        });
    }

    @SubscribeEvent
    public void onItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        if (failsDimensionCheck((PlayerEntity) evt.getEntityLiving())) return;
        TickManager.INSTANCE.doLater(() -> {
            final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();
            final ItemStack item = evt.getItem();

            final ExpressionContext expr = checkItem(item);
            if (expr == null) return;
            handle(player, item, expr);
        });
    }

    /**
     * Handles checking the item and adjusting the need for the player
     * @param item   The item to check
     */
    protected ExpressionContext checkItem(final ItemStack item) {
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : getItems().entrySet()) {
            if (!entry.getKey().test(item)) continue;
            return entry.getValue();
        }
        return null;
    }

    /**
     * Handles actually setting the value for the player
     * @param player The player
     * @param item   The source item
     * @param expr   The expression for the item
     */
    protected void handle(final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        setupExpression(() -> parent.getValue(player), player, item, expr);
        parent.adjustValue(player, expr.apply(player), this);
    }

    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : getItems().entrySet()) {
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

    public FoodExpressionContext getDefaultAmount() {
        return defaultAmount;
    }

    public Map<Predicate<ItemStack>, ExpressionContext> getItems() {
        return items;
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

    protected static class ItemValueDeserializer implements JsonDeserializer<Map<Predicate<ItemStack>, ExpressionContext>> {
        @Override
        public Map<Predicate<ItemStack>, ExpressionContext> deserialize(final JsonElement itemsEl, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return deserialize(itemsEl, context, FoodExpressionContext.class);
        }

        protected static Map<Predicate<ItemStack>, ExpressionContext> deserialize(final JsonElement itemsEl, final JsonDeserializationContext context, final Class<? extends FoodExpressionContext> contextClass) {
            final Map<Predicate<ItemStack>, ExpressionContext> output = new HashMap<>();
            // Handle if it's a key/value pair:
            if (itemsEl.isJsonObject()) {
                final JsonObject itemsObj = itemsEl.getAsJsonObject();
                itemsObj.entrySet().forEach((pair) -> {
                    final IIngredient ingredient = IIngredient.getIngredient(pair.getKey());
                    if(ingredient == null) return;

                    final JsonElement value = pair.getValue();
                    output.put(ingredient, !value.isJsonNull()
                            ? context.deserialize(value, contextClass)
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
                        amount = context.deserialize(elObj.get("amount"), contextClass);
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

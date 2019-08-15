package org.winterblade.minecraft.mods.needs.util.items;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.expressions.FoodExpressionContext;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ItemValueDeserializer implements JsonDeserializer<Map<Predicate<ItemStack>, ExpressionContext>> {
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

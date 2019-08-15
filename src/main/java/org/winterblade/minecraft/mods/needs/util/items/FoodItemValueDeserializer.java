package org.winterblade.minecraft.mods.needs.util.items;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.expressions.CountedFoodExpressionContext;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Predicate;

public class FoodItemValueDeserializer extends ItemValueDeserializer {
    @Override
    public Map<Predicate<ItemStack>, ExpressionContext> deserialize(final JsonElement itemsEl, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        return deserialize(itemsEl, context, CountedFoodExpressionContext.class);
    }
}

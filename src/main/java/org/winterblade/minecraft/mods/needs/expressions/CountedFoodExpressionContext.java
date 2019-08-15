package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(ExpressionContext.Deserializer.class)
public class CountedFoodExpressionContext extends FoodExpressionContext {
    public static final String COUNT = "count";

    protected static final Map<String, String> docs = new HashMap<>(FoodExpressionContext.docs);
    static {
        docs.put(COUNT, "The number of times this item has been used prior.");
    }

    public CountedFoodExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        final List<String> elements = super.getElements();
        elements.add(COUNT);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class FoodExpressionContext extends NeedExpressionContext {
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

package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class AndConditionalExpressionContext extends NeedExpressionContext {
    public static final String SOURCE = "source";
    protected static final Map<String, String> docs = new TreeMap<>(NeedExpressionContext.docs);

    static {
        docs.put("<condition key>", "The amount from the named condition, if any; e.g. if your condition had a" +
                "key of `swordchucks`, then you could write an expression like `pi * swordchucks + 5`.");
        docs.put(SOURCE, "The amount from the source trigger, if any.");
    }

    public AndConditionalExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add(SOURCE);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

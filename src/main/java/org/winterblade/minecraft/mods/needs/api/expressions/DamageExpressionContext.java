package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class DamageExpressionContext extends NeedExpressionContext {
    public static final String AMOUNT = "amount";
    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put(AMOUNT, "The amount of damage/healing.");
    }

    public DamageExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add(AMOUNT);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

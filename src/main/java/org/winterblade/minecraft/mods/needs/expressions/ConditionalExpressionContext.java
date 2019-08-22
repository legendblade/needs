package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class ConditionalExpressionContext extends NeedExpressionContext {
    public static final String MATCHED_VALUE = "matchedValue";
    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put(MATCHED_VALUE, "The amount from the matched condition, if any.");
    }

    public ConditionalExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add(MATCHED_VALUE);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

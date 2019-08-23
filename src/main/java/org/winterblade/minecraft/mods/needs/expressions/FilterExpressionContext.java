package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class FilterExpressionContext extends NeedExpressionContext {
    public static final String ADJUSTMENT = "adjustment";
    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put(ADJUSTMENT, "The amount of the adjustment.");
    }

    public FilterExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add(ADJUSTMENT);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

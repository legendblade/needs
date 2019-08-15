package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class OrConditionalExpressionContext extends NeedExpressionContext {
    public static final String MATCHED_VALUE = "matchedValue";
    public static final String SOURCE = "source";
    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put(MATCHED_VALUE, "The amount from the matched condition, if any.");
        docs.put(SOURCE, "The amount from the source trigger, if any.");
    }

    public OrConditionalExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add(MATCHED_VALUE);
        elements.add(SOURCE);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

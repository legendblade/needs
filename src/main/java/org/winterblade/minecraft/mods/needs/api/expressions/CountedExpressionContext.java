package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class CountedExpressionContext extends NeedExpressionContext {
    public static final String COUNT = "count";

    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put(COUNT, "The count of the value being tested.");
    }

    public CountedExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add("count");
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

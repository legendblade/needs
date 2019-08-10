package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class OtherNeedChangedExpressionContext extends NeedExpressionContext {
    public static final String OTHER = "other";
    public static final String PREVIOUS = "previous";
    public static final String CHANGE = "change";

    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);
    static {
        docs.put(OTHER, "The current value of the other need.");
        docs.put(PREVIOUS, "The previous value of the other need.");
        docs.put(CHANGE, "The amount the need changed by, or: (current - previous).");
    }

    public OtherNeedChangedExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        final List<String> elements = super.getElements();
        elements.add(OTHER);
        elements.add(PREVIOUS);
        elements.add(CHANGE);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

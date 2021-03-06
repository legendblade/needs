package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class OrConditionalExpressionContext extends ConditionalExpressionContext {
    public static final String SOURCE = "source";
    protected static final Map<String, String> docs = new HashMap<>(ConditionalExpressionContext.docs);

    static {
        docs.put(SOURCE, "The amount from the source trigger, if any.");
    }

    public OrConditionalExpressionContext() {
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

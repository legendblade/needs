package org.winterblade.minecraft.mods.needs.expressions;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class AndConditionalExpressionContext extends NeedExpressionContext {
    public static final int MATCH_COUNT = 20; // TODO: Config option?
    public static final String SOURCE = "source";
    protected static final Map<String, String> docs = new HashMap<>(NeedExpressionContext.docs);

    static {
        docs.put("match1", "The amount from the first matched condition, if any.");
        docs.put("match2", "The amount from the second matched condition, if any.");
        docs.put("matchN...", "The amount from the Nth matched condition, if any.");
        docs.put("match" + MATCH_COUNT, "The amount from the matched condition, if any.");
        docs.put(SOURCE, "The amount from the source trigger, if any.");
    }

    public AndConditionalExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        for (int i = 1; i <= MATCH_COUNT; i++) {
            elements.add("match" + i);
        }
        elements.add(SOURCE);
        return elements;
    }

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

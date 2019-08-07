package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.List;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class CountedExpressionContext extends NeedExpressionContext {
    public static final String COUNT = "count";

    public CountedExpressionContext() {
    }

    @Override
    public List<String> getElements() {
        elements.add("count");
        return elements;
    }
}

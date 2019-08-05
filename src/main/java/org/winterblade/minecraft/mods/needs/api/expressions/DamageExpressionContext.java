package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;

import java.util.List;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class DamageExpressionContext extends NeedExpressionContext {
    public static final String AMOUNT = "amount";

    public DamageExpressionContext() {
    }

    @Override
    protected List<String> getElements() {
        final List<String> elements = super.getElements();
        elements.add(AMOUNT);
        return elements;
    }
}

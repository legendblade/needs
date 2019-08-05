package org.winterblade.minecraft.mods.needs.api.expressions;

import java.util.List;

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

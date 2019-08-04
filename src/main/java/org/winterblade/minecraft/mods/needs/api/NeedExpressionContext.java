package org.winterblade.minecraft.mods.needs.api;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.ExpressionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class NeedExpressionContext extends ExpressionContext {
    public static final String CURRENT_NEED_VALUE = "current";

    public NeedExpressionContext() {
    }

    @Override
    protected List<String> getElements() {
        return new ArrayList<>(Arrays.asList(
            CURRENT_NEED_VALUE
        ));
    }
}

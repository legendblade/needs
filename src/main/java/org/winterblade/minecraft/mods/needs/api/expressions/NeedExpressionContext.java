package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class NeedExpressionContext extends ExpressionContext {
    public static final String CURRENT_NEED_VALUE = "current";
    private static final List<String> params = Collections.singletonList(
        CURRENT_NEED_VALUE
    );

    protected final ArrayList<String> elements = new ArrayList<>(params);

    public NeedExpressionContext() {
    }

    public void setCurrentNeedValue(final Need need, final PlayerEntity player) {
        setIfRequired(CURRENT_NEED_VALUE, () -> need.getValue(player));
    }

    @Override
    public List<String> getElements() {
        return elements;
    }
}

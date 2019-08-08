package org.winterblade.minecraft.mods.needs.api.expressions;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.*;

@JsonAdapter(ExpressionContext.Deserializer.class)
public class NeedExpressionContext extends ExpressionContext {
    public static final String CURRENT_NEED_VALUE = "current";
    private static final List<String> params = Collections.singletonList(
        CURRENT_NEED_VALUE
    );
    protected static final Map<String, String> docs = new HashMap<>();

    static {
        docs.put(CURRENT_NEED_VALUE, "The current value of the parent need.");
    }

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

    @Override
    public Map<String, String> getElementDocumentation() {
        return docs;
    }
}

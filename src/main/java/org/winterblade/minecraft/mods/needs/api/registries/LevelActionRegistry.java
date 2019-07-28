package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;

public class LevelActionRegistry extends TypedRegistry<LevelAction> {
    public static final LevelActionRegistry INSTANCE = new LevelActionRegistry();

    @Override
    public String getName() {
        return "Action";
    }

    @Override
    public LevelAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }
}

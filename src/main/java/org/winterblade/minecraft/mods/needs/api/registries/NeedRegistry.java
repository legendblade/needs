package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;

public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    @Override
    public String getName() {
        return "Need";
    }

    @Override
    public Need deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }
}

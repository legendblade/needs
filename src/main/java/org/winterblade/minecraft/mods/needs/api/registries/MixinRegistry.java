package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;

public class MixinRegistry extends TypedRegistry<IMixin> {
    public static final MixinRegistry INSTANCE = new MixinRegistry();

    @Override
    public String getName() {
        return "Mixin";
    }

    @Override
    public IMixin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }
}
package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;

public class ManipulatorRegistry extends TypedRegistry<IManipulator> {
    public static final ManipulatorRegistry INSTANCE = new ManipulatorRegistry();

    @Override
    public String getName() {
        return "Manipulator";
    }


    @Override
    public IManipulator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }
}

package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.Need;
import org.winterblade.minecraft.mods.needs.api.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;

import java.lang.reflect.Type;

public abstract class BaseManipulator implements IManipulator {
    protected Need parent;

    @Expose
    protected boolean silent;

    @Expose
    protected String messageFormat;

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public final void OnCreated(Need need) {
        parent = need;
        OnCreated();
    }

    public abstract void OnCreated();

    @Override
    public String FormatMessage(String needName, int amount, int newValue) {
        return String.format(
                messageFormat != null ? messageFormat : "Your %s has %s by %d to %d",
                needName.toLowerCase(),
                amount < 0 ? "decreased" : "increased",
                Math.abs(amount),
                newValue
        );
    }

    protected void CopyFrom(ItemUsedManipulator other) {
        silent = other.silent;
        messageFormat = other.messageFormat;
    }

    protected class Deserializer implements JsonDeserializer<IManipulator> {
        @Override
        public IManipulator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            // Short form for default settings
            if (json.isJsonPrimitive()) {
                String type = json.getAsString();
                if (type == null || type.isEmpty()) throw new JsonParseException("Bad manipulator " + json.toString() + ".");

                Class<? extends IManipulator> clazz = ManipulatorRegistry.GetType(type);
                if (clazz == null) throw new JsonParseException("Unknown manipulator '" + type + "'.");

                try {
                    return clazz.getConstructor().newInstance();
                } catch (Exception e) {
                    // This shouldn't happen
                    throw new JsonParseException("Unable to create a '" + type + "' manipulator using its short form.");
                }
            }

            // Extended form:
            if (!json.isJsonObject()) throw new JsonParseException("Manipulator must either be a string or an object.");
            JsonObject obj = json.getAsJsonObject();

            if (!obj.has("type")) throw new JsonParseException("Manipulator has no defined type.");

            JsonElement typeEl = obj.get("type");
            if (!typeEl.isJsonPrimitive()) throw new JsonParseException("Bad manipulator " + typeEl.toString() + ".");

            String type = typeEl.getAsString();
            if (type == null || type.isEmpty()) throw new JsonParseException("Bad manipulator " + typeEl.toString() + ".");

            Class<? extends IManipulator> clazz = ManipulatorRegistry.GetType(type);
            if (clazz == null) throw new JsonParseException("Unknown manipulator '" + type + "'.");

            return context.deserialize(json, clazz);
        }
    }
}

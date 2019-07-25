package org.winterblade.minecraft.mods.needs.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public abstract class TypedRegistry<T> implements JsonDeserializer<T> {
    private Map<String, Class<? extends T>> registry = new HashMap<>();

    public abstract String getName();

    public void register(String type, Class<? extends T> registrant, String... aliases) throws IllegalArgumentException {
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException(getName() + " key " + type + " is already in use by " + registrant.getCanonicalName());
        }

        registry.put(type.toLowerCase(), registrant);

        for (String alias : aliases) {
            register(alias, registrant);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Class<? extends T> getType(String type) {
        return registry.get(type.toLowerCase());
    }

    @SuppressWarnings("unused")
    public T doDeserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        // Short form for default settings
        if (json.isJsonPrimitive()) {
            String type = json.getAsString();
            if (type == null || type.isEmpty()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + json.toString() + ".");

            Class<? extends T> clazz = getType(type);
            if (clazz == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

            try {
                return clazz.getConstructor().newInstance();
            } catch (Exception e) {
                // This shouldn't happen
                throw new JsonParseException("Unable to create a '" + type + "' " + getName().toLowerCase() + " using its short form.");
            }
        }

        // Extended form:
        if (!json.isJsonObject()) throw new JsonParseException(getName() + " must either be a string or an object.");
        JsonObject obj = json.getAsJsonObject();

        if (!obj.has("type")) throw new JsonParseException(getName() + " must have a type property.");

        JsonElement typeEl = obj.get("type");
        if (!typeEl.isJsonPrimitive()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + typeEl.toString() + ".");

        String type = typeEl.getAsString();
        if (type == null || type.isEmpty()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + typeEl.toString() + ".");

        Class<? extends T> clazz = getType(type);
        if (clazz == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

        return context.deserialize(json, clazz);
    }
}

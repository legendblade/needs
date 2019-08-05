package org.winterblade.minecraft.mods.needs.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public abstract class TypedRegistry<T> implements JsonDeserializer<T> {
    protected Map<String, Class<? extends T>> registry = new HashMap<>();

    public abstract String getName();

    public void register(String type, final Class<? extends T> registrant, final String... aliases) throws IllegalArgumentException {
        type = type.toLowerCase();
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException(getName() + " key " + type + " is already in use by " + registrant.getCanonicalName());
        }

        registry.put(type, registrant);

        for (final String alias : aliases) {
            register(alias, registrant);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public Class<? extends T> getType(final String type) {
        return registry.get(type.toLowerCase());
    }

    @SuppressWarnings("unused")
    public T doDeserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {

        // Short form for default settings
        if (json.isJsonPrimitive()) {
            final String type = json.getAsString();
            if (type == null || type.isEmpty()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + json.toString() + ".");

            final Class<? extends T> clazz = getType(type);
            if (clazz == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

            try {
                return clazz.getConstructor().newInstance();
            } catch (final Exception e) {
                // This shouldn't happen
                throw new JsonParseException("Unable to create a '" + type + "' " + getName().toLowerCase() + " using its short form.");
            }
        }

        // Extended form:
        if (!json.isJsonObject()) throw new JsonParseException(getName() + " must either be a string or an object.");
        final JsonObject obj = json.getAsJsonObject();

        if (!obj.has("type")) throw new JsonParseException(getName() + " must have a type property.");

        final JsonElement typeEl = obj.get("type");
        if (!typeEl.isJsonPrimitive()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + typeEl.toString() + ".");

        final String type = typeEl.getAsString();
        if (type == null || type.isEmpty()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + typeEl.toString() + ".");

        final Class<? extends T> clazz = getType(type);
        if (clazz == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

        return context.deserialize(json, clazz);
    }
}

package org.winterblade.minecraft.mods.needs.util;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TypedRegistry<T> implements JsonDeserializer<T> {
    protected Map<String, Class<? extends T>> registry = new HashMap<>();
    private final Map<ResourceLocation, Class<? extends T>> registrantMap = new HashMap<>();

    public abstract String getName();

    /**
     * Register the given class under the ID, type, and aliases
     * @param modId      The mod registering this class; will build a {@link ResourceLocation} based on the mod name and
     *                   snake_cased type, then call {@link TypedRegistry#register(ResourceLocation, String, Class, String...)}
     * @param type       The primary name to register it as
     * @param registrant The class to register
     * @param aliases    Any aliases to also register it under
     * @throws IllegalArgumentException If the ID is contained in the registry or if any names for it are already in use.
     */
    public void register(final String modId, final String type, final Class<? extends T> registrant, final String... aliases) throws IllegalArgumentException {
        register(new ResourceLocation(modId, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, type)), type, registrant, aliases);
    }

    /**
     * Register the given class under the ID, type, and aliases
     * @param id         The ID for this class
     * @param type       The primary name to register it as
     * @param registrant The class to register
     * @param aliases    Any aliases to also register it under
     * @throws IllegalArgumentException If the ID is contained in the registry or if any names for it are already in use.
     */
    public void register(final ResourceLocation id, final String type, final Class<? extends T> registrant, final String... aliases) throws IllegalArgumentException {
        if (registrantMap.containsKey(id)) throw new IllegalArgumentException(getName() + " registry already contains an entry for " + id);

        registrantMap.put(id, registrant);
        register(type, registrant);
        for (final String alias : aliases) {
            register(alias, registrant);
        }
    }

    public ImmutableMap<String, Class<? extends T>> getRegistry() {
        return ImmutableMap.copyOf(registry);
    }

    public ImmutableMap<ResourceLocation, Class<? extends T>> getRegistrants() {
        return ImmutableMap.copyOf(registrantMap);
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

    /**
     * Add the registrant to the registry using the given type as the key
     * @param type       The type name
     * @param registrant The registrant to add
     * @throws IllegalArgumentException If the type already exists in the registry
     */
    private void register(String type, final Class<? extends T> registrant) throws IllegalArgumentException {
        type = type.toLowerCase();
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException(getName() + " key " + type + " is already in use by " + registrant.getCanonicalName());
        }

        registry.put(type, registrant);
    }
}

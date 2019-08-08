package org.winterblade.minecraft.mods.needs.util;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class TypedRegistry<T> implements JsonDeserializer<T> {
    private final Map<String, Tuple<Supplier<? extends T>, Class<? extends T>>> registry = new HashMap<>();
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
    public <S extends T> void register(final ResourceLocation id, final String type, final Class<S> registrant, final String... aliases) throws IllegalArgumentException {
        if (registrant == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

        register(id, type, registrant, () -> {
            try {
                return registrant.getConstructor().newInstance();
            } catch (final Exception e) {
                return null;
            }
        }, aliases);
    }

    /**
     * Register the given class under the ID, type, and aliases
     * @param id         The ID for this class
     * @param type       The primary name to register it as
     * @param clazz      The class to register
     * @param factory    A factory to create the given value
     * @param aliases    Any aliases to also register it under
     * @throws IllegalArgumentException If the ID is contained in the registry or if any names for it are already in use.
     */
    public <S extends T> void register(final ResourceLocation id, final String type, final Class<S> clazz, final Supplier<S> factory, final String... aliases) throws IllegalArgumentException {
        if (registrantMap.containsKey(id)) throw new IllegalArgumentException(getName() + " registry already contains an entry for " + id);

        registrantMap.put(id, clazz);
        register(type, clazz, factory);
        for (final String alias : aliases) {
            register(alias, clazz, factory);
        }
    }

    public ImmutableMap<String, Tuple<Supplier<? extends T>, Class<? extends T>>> getRegistry() {
        return ImmutableMap.copyOf(registry);
    }

    public ImmutableMap<ResourceLocation, Class<? extends T>> getRegistrants() {
        return ImmutableMap.copyOf(registrantMap);
    }

    @SuppressWarnings("WeakerAccess")
    public Supplier<? extends T> getFactory(final String type) {
        final Tuple<Supplier<? extends T>, Class<? extends T>> tuple = registry.get(type.toLowerCase());
        return tuple != null ? tuple.getA() : null;
    }

    @SuppressWarnings("WeakerAccess")
    public Class<? extends T> getType(final String type) {
        final Tuple<Supplier<? extends T>, Class<? extends T>> tuple = registry.get(type.toLowerCase());
        return tuple != null ? tuple.getB() : null;
    }

    @SuppressWarnings("unused")
    public T doDeserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {

        // Short form for default settings
        if (json.isJsonPrimitive()) {
            final String type = json.getAsString();
            if (type == null || type.isEmpty()) throw new JsonParseException("Bad " + getName().toLowerCase() + " " + json.toString() + ".");

            final Supplier<? extends T> factory = getFactory(type);
            if (factory == null) throw new JsonParseException("Unknown " + getName().toLowerCase() + " '" + type + "'.");

            try {
                return factory.get();
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
     * Add the factory to the registry using the given type as the key
     * @param type       The type name
     * @param clazz      The class of the registered object
     * @param factory    The factory to add
     * @throws IllegalArgumentException If the type already exists in the registry
     */
    private void register(String type, final Class<? extends T> clazz, final Supplier<? extends T> factory) throws IllegalArgumentException {
        type = type.toLowerCase();
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException(getName() + " key " + type + " is already in use by " + getType(type).getCanonicalName());
        }

        registry.put(type, new Tuple<>(factory, clazz));
    }
}

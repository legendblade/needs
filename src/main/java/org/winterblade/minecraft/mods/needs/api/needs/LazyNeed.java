package org.winterblade.minecraft.mods.needs.api.needs;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Contains a need which is lazy-loaded. While only minimal overhead exists in calling this repeatedly, users are
 * generally expected to ensure that they unregister any event listeners (esp. tick events) if this calls the invalid
 * function.
 */
@JsonAdapter(LazyNeed.Deserializer.class)
public class LazyNeed {
    private final String name;
    private Need instance;
    private boolean checked = false;

    /**
     * Creates a lazy-loaded need and registers it with the registry as a dependent to ensure is loaded.
     * @param name The name of the need to retrieve
     */
    @SuppressWarnings("WeakerAccess")
    public LazyNeed(final String name) {
        this.name = name;
        NeedRegistry.INSTANCE.registerDependentNeed(name);
    }

    /**
     * Gets the need, calling the first method with the need if it exists, and the second if it doesn't
     * @param present The consumer to call with the need if the need exists
     * @param invalid The function to call if it doesn't exist
     */
    public void get(final Consumer<Need> present, final Runnable invalid) {
        if (!checked) getInstance();
        if (instance != null) present.accept(instance);
        else invalid.run();
    }

    /**
     * Gets the need, calling the first method with the passed in variable and the need if it exists, and the
     * second method with nothing if it doesn't
     * @param present The consumer to call with the passed in variable and need if the need exists
     * @param invalid The function to call if it doesn't exist
     * @param t       The type of the variable to pass on
     */
    public <T> void get(final T t, final BiConsumer<Need, T> present, final Runnable invalid) {
        if (!checked) getInstance();
        if (instance != null) present.accept(instance, t);
        else invalid.run();
    }

    /**
     * Gets the need, calling the first method with the passed in variable and the need if it exists, and the
     * second method with only the passed in variable if it doesn't
     * @param present The consumer to call with the passed in variable and need if the need exists
     * @param invalid The function to call with the passed in variable if it doesn't exist
     * @param t       The type of the variable to pass on
     */
    public <T> void get(final T t, final BiConsumer<Need, T> present, final Consumer<T> invalid) {
        if (!checked) getInstance();
        if (instance != null) present.accept(instance, t);
        else invalid.accept(t);
    }

    private void getInstance() {
        instance = NeedRegistry.INSTANCE.getByName(name);
        checked = true;
    }

    protected static class Deserializer implements JsonDeserializer<LazyNeed> {

        @Override
        public LazyNeed deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonPrimitive()) throw new JsonParseException("Need must be a string");

            final JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (!primitive.isString()) throw new JsonParseException("Need must be a string");

            return new LazyNeed(primitive.getAsString());
        }
    }
}

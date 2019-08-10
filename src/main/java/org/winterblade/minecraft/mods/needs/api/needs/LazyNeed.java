package org.winterblade.minecraft.mods.needs.api.needs;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.lang.reflect.Type;
import java.util.Objects;
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
     * Creates a LazyNeed that always returns the given need
     * @param need The need
     * @return The lazy need
     */
    public static LazyNeed of(final Need need) {
        return new UnlazyNeed(need);
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

    /**
     * Checks that the passed in need is not the same as this need
     * @param need The need to check against
     * @return True if the needs are different, false otherwise
     */
    public boolean isNot(final Need need) {
        if (instance != null) return instance == need;
        return !need.getName().equals(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final LazyNeed lazyNeed = (LazyNeed) o;

        return instance != null
                ? Objects.equals(instance, lazyNeed.instance)
                : Objects.equals(name, lazyNeed.name); // TODO: Technically this allows for two separate aliases
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, instance);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Discards the stored value
     */
    public void discard() {
        instance = null;
        checked = false;
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

    private static class UnlazyNeed extends LazyNeed {
        private final Need need;

        public UnlazyNeed(final Need need) {
            super(need.getName());
            this.need = need;
        }

        @Override
        public void get(final Consumer<Need> present, final Runnable invalid) {
            present.accept(need);
        }

        @Override
        public <T> void get(final T t, final BiConsumer<Need, T> present, final Runnable invalid) {
            present.accept(need, t);
        }

        @Override
        public <T> void get(final T t, final BiConsumer<Need, T> present, final Consumer<T> invalid) {
            present.accept(need, t);
        }
    }
}

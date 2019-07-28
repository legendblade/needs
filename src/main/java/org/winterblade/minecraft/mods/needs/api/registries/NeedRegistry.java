package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("WeakerAccess")
public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    private final Set<String> dependencies = new HashSet<>();
    private final Set<Need> loaded = new HashSet<>();

    @Override
    public String getName() {
        return "Need";
    }

    @Override
    public Need deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }

    /**
     * Checks if the need is valid and should be loaded
     * @param need The need to register
     * @return     True if the need can be registered, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(Need need) {
        Predicate<Need> predicate = (n) -> n.getName().equals(need.getName());

        if (!need.allowMultiple()) {
            predicate = predicate.or((n) -> n.getClass().equals(need.getClass()));
        }

        return loaded.stream().noneMatch(predicate);
    }

    public void registerDependentNeed(String name) {
        dependencies.add(name);
    }

    /**
     * Register the created need
     * @param need The need to register
     * @throws IllegalArgumentException If the need could not be registered because it isn't valid
     */
    public void register(Need need) throws IllegalArgumentException {
        if (!isValid(need)) throw new IllegalArgumentException("Tried to register need of same name or singleton with same class.");
        loaded.add(need);
    }

    public void validateDependencies() {
        dependencies.forEach((d) -> {
            if (loaded.stream().anyMatch((n) -> n.getName().equals(d))) return;

            NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't loaded while validating dependencies of other needs.");
        });
    }
}

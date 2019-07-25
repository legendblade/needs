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

    public void registerDependentNeed(String needType) {
        if (getType(needType) == null) {
            NeedsMod.LOGGER.error("Need of type " + needType + " was requested as a dependency but no type of that name was found.");
            return;
        }
        dependencies.add(needType);
    }

    public void cache(Need need) {
        loaded.add(need);
    }

    public void cacheDependencies() {
        dependencies.forEach((d) -> {
            Class<? extends Need> type = getType(d);

            // Skip already loaded deps
            if (loaded.stream().anyMatch((l) -> type.isAssignableFrom(l.getClass()))) return;

            try {
                loaded.add(type.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                NeedsMod.LOGGER.error("Unable to load dependency " + d, e);
            }
        });
    }
}

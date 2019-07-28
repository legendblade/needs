package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("WeakerAccess")
public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    private final Set<String> dependencies = new HashSet<>();
    private final Set<Need> loaded = new HashSet<>();
    private final WeakHashMap<Need, Consumer<PlayerEntity>> perPlayerTick = new WeakHashMap<>();

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

            // Check if the name is a type:
            Class<? extends Need> type = getType(d);
            if (type != null) {
                if (loaded.stream().anyMatch((l) -> type.isAssignableFrom(l.getClass()))) {
                    NeedsMod.LOGGER.error("Dependency '" + d + "' would have loaded a type of the same name, but one is already registered.");
                    return;
                }

                try {
                    Need need = type.newInstance();
                    loaded.add(need);
                    need.finalizeDeserialization();
                    return;
                } catch (InstantiationException | IllegalAccessException e) {
                    NeedsMod.LOGGER.error("Unable to load dependency " + d, e);
                }
            }

            NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't loaded while validating dependencies of other needs.");
        });
    }

    /**
     * Gets a need by name, falling back to type otherwise
     * @param need  The name of the need
     * @return      The need matching that name or type.
     */
    public Need getByName(String need) {
        return loaded
                .stream()
                .filter((n) -> n.getName().equals(need))
                .findFirst()
                .orElse(loaded
                        .stream()
                        .filter((n) ->
                            registry
                                .entrySet()
                                .stream()
                                .anyMatch((rn) -> rn.getValue().equals(n.getClass()) && rn.getKey().equals(need))
                        )
                        .findFirst()
                        .orElse(null)
                );
    }

    /**
     * Sets an action to get called for this need, every 5 ticks, per player
     * @param need   The need to register
     * @param action The action to call
     */
    public void requestPlayerTickUpdate(Need need, Consumer<PlayerEntity> action) {
        // Register us on the first event added:
        if (perPlayerTick.size() <= 0) MinecraftForge.EVENT_BUS.addListener(this::onTick);

        perPlayerTick.put(need, action);
    }

    /**
     * Called when the world ticks, if we have a listener that needs it
     * @param event The tick event
     */
    private void onTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END || (event.world.getGameTime() % 5) != 0) return;

        event.world
            .getPlayers()
            .forEach((p) -> perPlayerTick.forEach((key, value) -> value.accept(p)));
    }
}

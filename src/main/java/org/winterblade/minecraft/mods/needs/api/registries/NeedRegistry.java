package org.winterblade.minecraft.mods.needs.api.registries;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Tuple;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.network.ConfigCheckPacket;
import org.winterblade.minecraft.mods.needs.network.ConfigDesyncPacket;
import org.winterblade.minecraft.mods.needs.network.ConfigSyncedPacket;
import org.winterblade.minecraft.mods.needs.network.NetworkManager;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    private final Set<String> dependencies = new HashSet<>();
    private final Set<Need> loaded = new HashSet<>();
    private final WeakHashMap<Need, Consumer<PlayerEntity>> perPlayerTick = new WeakHashMap<>();
    private boolean shouldSync = false;

    /**
     * Client-side cache
     */
    private final Map<String, LocalCachedNeed> localCache = new ConcurrentHashMap<>();

    /**
     * Config storage; client and server both have hashes, server has full configs
     */
    private final Map<String, byte[]> cachedConfigHashes = new HashMap<>();
    private final Map<String, String> cachedConfigs = new HashMap<>();

    @Override
    public String getName() {
        return "Need";
    }

    @Override
    public Need deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        return INSTANCE.doDeserialize(json, typeOfT, context);
    }

    /**
     * Checks if the need is valid and should be loaded
     * @param need The need to register
     * @return     True if the need can be registered, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(final Need need) {
        Predicate<Need> predicate = (n) -> n.getName().equals(need.getName());

        if (!need.allowMultiple()) {
            predicate = predicate.or((n) -> n.getClass().equals(need.getClass()));
        }

        return loaded.stream().noneMatch(predicate);
    }

    public void registerDependentNeed(final String name) {
        dependencies.add(name);
    }

    /**
     * Register the created need
     * @param need The need to register
     * @throws IllegalArgumentException If the need could not be registered because it isn't valid
     */
    public void register(final Need need) throws IllegalArgumentException {
        if (!isValid(need)) throw new IllegalArgumentException("Tried to register need of same name or singleton with same class.");
        loaded.add(need);
    }

    /**
     * Registers a need from a file
     * @param need    The need
     * @param id      The ID
     * @param digest  The digest of the file
     * @param content The file content, if on the server
     */
    public void register(final Need need, final String id, final byte[] digest, @Nullable final String content) {
        register(need);
        cachedConfigHashes.put(id, digest);
        if (content != null) cachedConfigs.put(id, content);
    }

    public void validateDependencies() {
        dependencies.forEach((d) -> {
            if (loaded.stream().anyMatch((n) -> n.getName().equals(d))) return;

            // Check if the name is a type:
            final Class<? extends Need> type = getType(d);
            if (type != null) {
                if (loaded.stream().anyMatch((l) -> type.isAssignableFrom(l.getClass()))) {
                    NeedsMod.LOGGER.error("Dependency '" + d + "' would have loaded a type of the same name, but one is already registered.");
                    return;
                }

                try {
                    final Need need = type.newInstance();
                    loaded.add(need);
                    need.finalizeDeserialization();
                    return;
                } catch (final InstantiationException | IllegalAccessException e) {
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
    @Nullable
    public Need getByName(final String need) {
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
    public void requestPlayerTickUpdate(final Need need, final Consumer<PlayerEntity> action) {
        // Register us on the first event added:
        if (perPlayerTick.size() <= 0) MinecraftForge.EVENT_BUS.addListener(this::onTick);

        perPlayerTick.put(need, action);
    }

    /**
     * Registers that a need needs to be synced down from the server to the client.
     *
     * Without at least one need being synced, the system will not transmit any update packets to the player.
     *
     * If you do anything client side with a need, you should register it here and work with the standard sync process.
     */
    public void registerForSync() {
        shouldSync = true;
    }

    /**
     * Called on the client side when a need's value is changed by the server
     * @param name  The need ID
     * @param value The new value
     * @param min   The min value of the need for this player
     * @param max   The max value of the need for this player
     */
    public void setLocalNeed(final String name, final double value, final double min, final double max) {
        localCache.compute(name, (k, v) -> {
            if (v != null) {
                v.setValue(value);
                v.setMin(min);
                v.setMax(max);
                return v;
            }

            MinecraftForge.EVENT_BUS.post(new LocalCacheUpdatedEvent());

            final Need need = getByName(name);
            if (need == null) return null;

            return new LocalCachedNeed(need, value, min, max);
        });
    }

    /**
     * Gets an immutable copy of the local cahce
     * @return The cache
     */
    public Map<String, LocalCachedNeed> getLocalCache() {
        return ImmutableMap.copyOf(localCache);
    }

    /**
     * Called on the client side to validate config hashes
     * @param configHashes The map of ID to file hash
     */
    public void validateConfig(final Map<String, byte[]> configHashes) {
        final Map<String, ConfigDesyncPacket.DesyncTypes> desyncs = configHashes
                .entrySet()
                .stream()
                .map((kv) -> {
                    boolean exists = cachedConfigHashes.containsKey(kv.getKey());
                    if (exists && Arrays.equals(cachedConfigHashes.get(kv.getKey()), kv.getValue())) return null;
                    return new Tuple<>(kv.getKey(), exists
                            ? ConfigDesyncPacket.DesyncTypes.BAD_HASH
                            : ConfigDesyncPacket.DesyncTypes.MISSING);
                })
                .filter(Objects::nonNull)
                // TODO: Check local cache
                .collect(Collectors.toMap(Tuple::getA, Tuple::getB));

        NetworkManager.INSTANCE.sendToServer(
            desyncs.size() <= 0
                ? new ConfigSyncedPacket()
                : new ConfigDesyncPacket(desyncs)
        );
    }

    /**
     * Called when the player logs in to the server
     * @param event The login event
     */
    public void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

        // Only run config sync when on the dedicated server; if you manage to desync configs on a client... how?
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> NetworkManager.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
            new ConfigCheckPacket(cachedConfigHashes)
        ));

        // Shortcut the entire system when it's just the local client:
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            // Yes, I know, this is breaking rules about cross access from server to client
            onConfigSynced(event.getPlayer(),
                (n, v) -> setLocalNeed(n.getName(), v, n.getMin(event.getPlayer()), n.getMax(event.getPlayer()))
            );
        });
    }

    /**
     * Called once a player is done receiving all updated configs
     * @param player   The player
     * @param callback Callback invoked with the value of a single need
     */
    public void onConfigSynced(final PlayerEntity player, final BiConsumer<Need, Double> callback) {
        // TODO: I'd prefer to sync slowly instead of all at once; figure out a way to appropriately run later
        loaded.forEach((n) -> {
            // Only sync the need values if it's required on the clietn
            if(!n.shouldSync()) return;

            callback.accept(n, n.getValue(player));
        });
    }

    /**
     * Called when the world ticks, if we have a listener that needs it. This uses the world tick rather than
     * the player tick to avoid %5'ing in every player
     * @param event The tick event
     */
    private void onTick(final TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END || (event.world.getGameTime() % 5) != 0) return;

        event.world
            .getPlayers()
            .forEach((p) -> perPlayerTick.forEach((key, value) -> value.accept(p)));
    }
}

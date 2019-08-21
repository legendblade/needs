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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    private final Queue<String> dependencies = new LinkedList<>();
    private final Set<Need> instances = new HashSet<>();

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

        return instances.stream().noneMatch(predicate);
    }

    /**
     * Registers that a need is used as a dependency; these will get automatically resolved after all needs have
     * been registered. This is automatically called when using a {@link LazyNeed}.
     * @param name The name of the need
     */
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
        instances.add(need);
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
        while (!dependencies.isEmpty()) {
            final String d = dependencies.poll();
            if (d == null) continue;

            try {
                if (instances.stream().anyMatch((n) -> n.getName().equals(d))) continue;

                // Check if the name is a type:
                final Supplier<? extends Need> factory = getFactory(d);
                if (factory != null) {
                    if (instances.stream().anyMatch((l) -> getType(d).isAssignableFrom(l.getClass()))) {
                        NeedsMod.LOGGER.error("Dependency '" + d + "' would have loaded a type of the same name, but one is already registered.");
                        continue;
                    }

                    final Need need = factory.get();
                    if (need == null) {
                        NeedsMod.LOGGER.error("A need of name '" + d + "' could not be created while validating dependencies of other needs.");
                        continue;
                    }

                    try {
                        need.beginValidate();
                    } catch (final IllegalArgumentException e) {
                        NeedsMod.LOGGER.error("A need of name '" + d + "' was in an invalid state while loading dependencies: " + e.getMessage(), e);
                    }
                    instances.add(need);
                    continue;
                }

                NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't able to be loaded while validating dependencies of other needs.");
            } catch (final Exception e) {
                NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't able to be loaded while validating dependencies of other needs.", e);
            }
        }
    }

    /**
     * Gets a need by name
     * @param need  The name of the need
     * @return      The need matching that name or null if not found.
     */
    @Nullable
    public Need getByName(final String need) {
        return instances
                .stream()
                .filter((n) -> n.getName().toLowerCase().equals(need.toLowerCase()))
                .findFirst()
                .orElseGet(() -> {
                    final Tuple<Supplier<? extends Need>, Class<? extends Need>> val = getRegistry().get(need.toLowerCase());
                    if (val == null) return null;

                    return instances
                        .stream()
                        .filter((n) -> n.getClass().equals(val.getB()))
                        .findFirst()
                        .orElse(null);
                });
    }

    /**
     * Registers that a need needs to be synced down from the server to the client.
     *
     * Without at least one need being synced, the system will not transmit any update packets to the player.
     *
     * If you do anything client side with a need, you should register it here and work with the standard sync process.
     */
    public void registerForSync() {
    }

    /**
     * Called on the client side when a need's value is changed by the server
     * @param name  The need ID
     * @param value The new value
     * @param min   The min value of the need for this player
     * @param max   The max value of the need for this player
     */
    public void setLocalNeed(final String name, final double value, final double min, final double max) {
        final AtomicBoolean updated = new AtomicBoolean();
        localCache.compute(name.toLowerCase(), (k, v) -> {
            if (v != null) {
                v.setMin(min);
                v.setMax(max);
                v.setValue(value);
                return v;
            }

            final Need need = getByName(name);
            if (need == null) return null;

            updated.set(true);
            return new LocalCachedNeed(need, value, min, max);
        });

        if (updated.get()) MinecraftForge.EVENT_BUS.post(new LocalCacheUpdatedEvent());
    }

    /**
     * Gets an immutable copy of the local cache
     * @return The cache
     */
    public Map<String, LocalCachedNeed> getLocalCache() {
        return ImmutableMap.copyOf(localCache);
    }

    /**
     * Gets a need from the local cache
     * @return The local need
     */
    public LocalCachedNeed getLocalNeed(final String name) {
        return localCache.get(name.toLowerCase());
    }

    /**
     * Called on the client side to validate config hashes
     * @param configHashes The map of ID to file hash
     */
    public void validateConfig(final Map<String, byte[]> configHashes) {
        localCache.clear(); // This is the first packet we get on login, so clear the cache here.

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

        if (desyncs.isEmpty()) {
            instances.forEach(need -> {
                try {
                    need.finishLoad();
                } catch (final Exception e) {
                    NeedsMod.LOGGER.error("Error loading need.", e);
                }
            });
        }
    }

    /**
     * Called when the player logs in to the server
     * @param event The login event
     */
    public void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;

        // Make sure all our needs are initialized
        instances.forEach((n) -> n.setInitial(event.getPlayer()));

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
        // Alternatively, sync all values in a single packet.
        instances.forEach((n) -> {
            // Only sync the need values if it's required on the clietn
            if(!n.shouldSync()) return;

            callback.accept(n, n.getValue(player));
        });
    }

    public void onServerStarted(@SuppressWarnings("unused") final FMLServerStartedEvent event) {
        instances.forEach(Need::finishLoad);
    }

    @SuppressWarnings("unused")
    public void onServerStopped(final FMLServerStoppedEvent event) {
        localCache.clear(); // This only applies to the local side
        instances.forEach(Need::beginUnload);
    }
}

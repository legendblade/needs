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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;
import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.config.NeedInitializer;
import org.winterblade.minecraft.mods.needs.config.RemoteFileHandler;
import org.winterblade.minecraft.mods.needs.network.*;
import org.winterblade.minecraft.mods.needs.util.TypedRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class NeedRegistry extends TypedRegistry<Need> {
    public static final NeedRegistry INSTANCE = new NeedRegistry();

    private final Queue<String> dependencies = new LinkedList<>();
    private final Set<Need> loaded = new HashSet<>();

    /**
     * Client-side cache
     */
    private final Map<String, LocalCachedNeed> localCache = new ConcurrentHashMap<>();

    /**
     * Config storage; client and server both have cached hashes, server has full configs.
     * Client will record the current config hashes from the server to compare for later
     */
    private final Map<String, byte[]> cachedConfigHashes = new HashMap<>();
    private final Map<String, byte[]> remoteConfigHashes = new HashMap<>();
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
        while (!dependencies.isEmpty()) {
            final String d = dependencies.remove();
            if (loaded.stream().anyMatch((n) -> n.getName().equals(d))) continue;

            // Check if the name is a type:
            final Supplier<? extends Need> factory = getFactory(d);
            if (factory != null) {
                if (loaded.stream().anyMatch((l) -> getType(d).isAssignableFrom(l.getClass()))) {
                    NeedsMod.LOGGER.error("Dependency '" + d + "' would have loaded a type of the same name, but one is already registered.");
                    continue;
                }

                final Need need = factory.get();
                if (need == null) {
                    NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't loaded while validating dependencies of other needs.");
                    continue;
                }

                loaded.add(need);
                need.finishLoad();
                continue;
            }

            NeedsMod.LOGGER.error("A need of name '" + d + "' wasn't loaded while validating dependencies of other needs.");
        }
    }

    /**
     * Gets a need by name
     * @param need  The name of the need
     * @return      The need matching that name or null if not found.
     */
    @Nullable
    public Need getByName(final String need) {
        return loaded
                .stream()
                .filter((n) -> n.getName().equals(need))
                .findFirst()
                .orElse(null);
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
        remoteConfigHashes.clear();
        final Map<String, ConfigDesyncPacket.DesyncTypes> desyncs = configHashes
                .entrySet()
                .stream()
                .peek((kv) -> remoteConfigHashes.put(kv.getKey(), kv.getValue()))
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

        // Make sure all our needs are initialized
        loaded.forEach((n) -> n.setInitial(event.getPlayer()));

        // Only run config sync when on the dedicated server; if you manage to desync configs on a client... how?
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> sendConfigHashesToPlayer((ServerPlayerEntity) event.getPlayer()));

        // Shortcut the entire system when it's just the local client:
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            // Yes, I know, this is breaking rules about cross access from server to client
            onConfigSynced(event.getPlayer(),
                (n, v) -> setLocalNeed(n.getName(), v, n.getMin(event.getPlayer()), n.getMax(event.getPlayer()))
            );
        });
    }

    /**
     * Sends the config hashes to the player
     * @param player The player to send to
     */
    public void sendConfigHashesToPlayer(final ServerPlayerEntity player) {
        NetworkManager.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ConfigCheckPacket(
                    // We only need to sync configs that are needed by the client.
                    loaded
                        .stream()
                        .filter(Need::shouldSync)
                        .map((n) -> new Tuple<>(n.getName(), cachedConfigHashes.get(n.getName())))
                        .collect(Collectors.toMap(Tuple::getA, Tuple::getB))
                )
        );
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
     * Gets the saved config
     *
     * We're loading it from a cache rather than trying to load it from the filesystem to shortcut any
     * possibility that a client can attempt to request other server files.
     * @param needId The need ID
     * @return The cached config JSON
     */
    public String getContent(final String needId) {
        return cachedConfigs.get(needId);
    }

    /**
     * Updates a local need from the remote
     * @param needId  The need ID
     * @param content The content of the file
     */
    public void updateFromRemote(final String needId, final String content) {
        // Did we not want this one? Chuck it out.
        if (!remoteConfigHashes.containsKey(needId)) NetworkManager.INSTANCE.sendToServer(new ConfigTransferSuccess());

        // First, make sure this need matches what we were expecting to receive:
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(content.getBytes());

            if (!Arrays.equals(remoteConfigHashes.get(needId), digest)) {
                NeedsMod.LOGGER.error("Unable to validate need " + needId + " from server; " +
                        "expected: " + Arrays.toString(remoteConfigHashes.get(needId)) +
                        ", got: " + Arrays.toString(digest));
                NetworkManager.INSTANCE.sendToServer(new ConfigTransferFailure(needId));
                return;
            }
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Hand this off, because the next bit runs client-only code...
        final Need need = NeedInitializer.INSTANCE.deserializeRemoteContent(needId, content);
        if (need == null) {
            NetworkManager.INSTANCE.sendToServer(new ConfigTransferFailure(needId));
            return;
        }



        DistExecutor.runWhenOn(Dist.CLIENT, () ->
            () ->
                NetworkManager.INSTANCE.sendToServer(
                    RemoteFileHandler.handle(needId, content)
                        ? new ConfigTransferSuccess()
                        : new ConfigTransferFailure(needId)
        ));
    }
}

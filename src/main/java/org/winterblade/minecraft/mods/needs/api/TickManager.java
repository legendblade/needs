package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.config.CoreConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TickManager {
    public static TickManager INSTANCE = new TickManager();
    private final int buckets;

    private final WeakHashMap<Object, Consumer<PlayerEntity>> perPlayerTick = new WeakHashMap<>();
    private final List<List<PlayerEntity>> players = new ArrayList<>();

    private int currentBucket = 0;

    private TickManager() {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);

        // Init our buckets
        buckets = Math.max(CoreConfig.COMMON.tickBuckets.get(), 1);
        for(int i = 0; i < buckets; i++) {
            players.add(new ArrayList<>());
        }
    }

    /**
     * Sets an action to get called for this need, every 5 ticks, per player
     * @param source The source of the action
     * @param action The action to call
     */
    public void requestPlayerTickUpdate(final Object source, final Consumer<PlayerEntity> action) {
        // Register us on the first event added:
        if (perPlayerTick.size() <= 0) MinecraftForge.EVENT_BUS.addListener(this::onTick);

        perPlayerTick.put(source, action);
    }

    public void removePlayerTickUpdate(final Object source) {
        perPlayerTick.remove(source);
    }

    /**
     * Enqueues a task to run on the server whenever it feels like it; this should generally
     * only be called from the server. This will always delay the task.
     * @param runnable The runnable
     */
    public CompletableFuture<Void> doLater(final Runnable runnable) {
        final ThreadTaskExecutor<?> executor = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);

        if (executor != null) {
            return executor.deferTask(runnable);
        } else {
            NeedsMod.LOGGER.error("doLater called from a context where there is no server.");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Gets the number of buckets (ie: number of ticks between player updates)
     * @return  The bucket count.
     */
    public int getBucketCount() {
        return buckets;
    }

    /**
     * Called when the server ticks
     * @param event The tick event
     */
    private void onTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        players.get(currentBucket++).forEach((p) -> {
            if (p == null) return; // How?
            perPlayerTick.forEach((k, v) -> v.accept(p));
        });
        if (buckets <= currentBucket) currentBucket = 0;
    }

    /**
     * Called when a player joins the world
     * @param event The event
     */
    private void onPlayerJoin(@SuppressWarnings("unused") final PlayerEvent.PlayerLoggedInEvent event) {
        // Fired after the player is added to the server player list
        players.stream().min(Comparator.comparingInt(List::size)).ifPresent((l) -> l.add(event.getPlayer()));
    }

    /**
     * Called when a player leaves the world
     * @param event The event
     */
    private void onPlayerLeave(@SuppressWarnings("unused") final PlayerEvent.PlayerLoggedOutEvent event) {
        // Fired _before_ the player is removed from the player list
        for (final List<PlayerEntity> bucket : players) {
            if(bucket.remove(event.getPlayer())) return;
        }

        // TODO: Consider if we should resort buckets
    }
}

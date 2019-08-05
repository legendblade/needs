package org.winterblade.minecraft.mods.needs.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class TickManager {
    public static TickManager INSTANCE = new TickManager();
    private static final int buckets = 5; // TODO: This needs to be a config option

    private final List<Consumer<PlayerEntity>> perPlayerTick = new ArrayList<>();
    private final List<List<PlayerEntity>> players = new ArrayList<>();

    private int currentBucket = 0;

    private TickManager() {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);

        // Init our buckets
        for(int i = 0; i < buckets; i++) {
            players.add(new ArrayList<>());
        }
    }

    /**
     * Sets an action to get called for this need, every 5 ticks, per player
     * @param action The action to call
     */
    public void requestPlayerTickUpdate(final Consumer<PlayerEntity> action) {
        // Register us on the first event added:
        if (perPlayerTick.size() <= 0) MinecraftForge.EVENT_BUS.addListener(this::onTick);

        perPlayerTick.add(action);
    }

    /**
     * Called when the server ticks
     * @param event The tick event
     */
    private void onTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        players.get(currentBucket++).forEach((p) -> perPlayerTick.forEach((a) -> a.accept(p)));
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
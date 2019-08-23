package org.winterblade.minecraft.mods.needs.api.needs;

import com.google.common.collect.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.network.PacketDistributor;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedInitializationEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedLevelEvent;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.network.NeedUpdatePacket;
import org.winterblade.minecraft.mods.needs.network.NetworkManager;
import org.winterblade.minecraft.mods.needs.util.MathLib;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The base implementation of a need; values should only be read/written to this on the server; clients will have
 * a local copy of the need values in a LocalCachedNeed.
 *
 * If you need to access the need on the client side, be sure to call {@link Need#enableSyncing()} to tell the
 * registry to send update events to the client.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@JsonAdapter(NeedRegistry.class)
public abstract class Need {
    @Expose
    private List<IManipulator> manipulators = Collections.emptyList();

    @Expose
    private List<IMixin> mixins = Collections.emptyList();

    @SuppressWarnings("FieldMayBeFinal")
    @Expose
    @SerializedName("levels")
    private List<NeedLevel> levelList = Collections.emptyList();

    private RangeMap<Double, NeedLevel> levels = TreeRangeMap.create();
    private boolean shouldSync;

    /**
     * Determines if a need should be able to be declared more than once.
     * Override this anytime it makes sense to have multiple needs of the same class
     * @return  True if this can be declared more than once; false otherwise
     */
    public boolean allowMultiple() {
        return false;
    }

    public final void beginValidate() throws IllegalArgumentException
    {
        getManipulators().forEach((m) -> m.validate(this));
        getMixins().forEach((m) -> m.validate(this));

        // Ensure our levels are properly defined and don't overlap:
        for (final NeedLevel l : levelList) {
            final RangeMap<Double, NeedLevel> subRange = levels.subRangeMap(l.getRange());
            if (0 < subRange.asMapOfRanges().size()) {
                throw new IllegalArgumentException("This need has overlapping levels; ensure that min/max ranges do not overlap");
            }

            levels.put(l.getRange(), l);
        }

        for (final Map.Entry<Range<Double>,NeedLevel> kv : getLevels().entrySet()) {
            kv.getValue().validate(this);
        }

        validate();
    }

    /**
     * Called after deserialization to validate the need has everything it needs
     * to function.
     * @throws IllegalArgumentException If a parameter is invalid
     */
    public void validate() throws IllegalArgumentException {
    }

    /**
     * Called in order to finish loading the need and its children
     */
    public final void finishLoad() {
        // Freeze our manipulator list
        manipulators = ImmutableList.copyOf(manipulators);
        mixins = ImmutableList.copyOf(mixins);
        levels = ImmutableRangeMap.copyOf(levels);

        // Finalize creating everything:
        getManipulators().forEach((m) -> {
            m.onLoaded(this);
            MinecraftForge.EVENT_BUS.register(m);
        });

        getMixins().forEach((m) -> {
            m.onLoaded(this);
            MinecraftForge.EVENT_BUS.register(m);
        });

        boolean hasTickingActions = false;
        for (final Map.Entry<Range<Double>,NeedLevel> kv : getLevels().entrySet()) {
            final NeedLevel v = kv.getValue();
            v.onLoaded(this);
            MinecraftForge.EVENT_BUS.register(v);
            hasTickingActions = hasTickingActions || v.hasTickingActions();
        }

        // Let subclasses wrap up anything they need to:
        onLoaded();

        // Register ourself where necessary:
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onRespawned);
        if (hasTickingActions) MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onPlayerJoined);
    }

    /**
     * Fired when a need is loaded
     */
    public void onLoaded() {

    }

    /**
     * Called when a need is about to be unloaded.
     */
    public final void beginUnload() {
        getManipulators().forEach(manipulator -> {
            MinecraftForge.EVENT_BUS.unregister(manipulator);
            manipulator.onUnloaded();
        });

        getMixins().forEach(mixin -> {
            MinecraftForge.EVENT_BUS.unregister(mixin);
            mixin.onUnloaded();
        });

        for (final Map.Entry<Range<Double>,NeedLevel> kv : getLevels().entrySet()) {
            final NeedLevel level = kv.getValue();
            MinecraftForge.EVENT_BUS.unregister(level);
            level.onUnloaded();
        }

        onUnloaded();

        // Bye-bye
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    /**
     * Fired when a need is unloaded
     */
    public void onUnloaded() {

    }

    /**
     * Wrapper implementation to cast the entity to a player
     * @param entity    The entity to check
     * @param adjust    The amount to adjust by
     * @param source    The source of the adjustment
     */
    public final void adjustValue(final Entity entity, final double adjust, final IManipulator source) {
        if (!(entity instanceof PlayerEntity)) return;
        adjustValue((PlayerEntity) entity, adjust, source);
    }

    /**
     * Adjust the value of this need for a given player
     * @param player      The player to adjust
     * @param adjust      The amount to adjust by
     * @param source      The source of the adjustment
     */
    public final double adjustValue(final PlayerEntity player, final double adjust, final IManipulator source) {
        if (player == null || adjust == 0 || player.world.isRemote) return 0;
        if (!setInitial(player)) return 0;

        // Check if we shouldn't adjust the player at this moment
        final double current = getValue(player);
        final NeedAdjustmentEvent.Pre event = new NeedAdjustmentEvent.Pre(this, player, source, adjust);
        if (MinecraftForge.EVENT_BUS.post(event) || event.getAmount() == 0) return current;

        // Get our clamped value:
        double newValue = MathLib.clamp(current + event.getAmount(), getMin(player), getMax(player), source.getLowestToSetNeed(), source.getHighestToSetNeed());

        // If the new value is the same as the current because we've hit the max/min, don't do anything
        if (newValue == current) return newValue;

        // Finally, set the value...
        newValue = setValue(player, newValue, adjust);
        if (newValue == current) return newValue; // If we didn't _really_ change, bail

        // ... and let our listeners know
        sendUpdates(player, source, current, newValue);
        return newValue;
    }

    /**
     * Initializes the value if necessary
     * @param player The player to set
     * @return False if the need was not initialized; true otherwise
     */
    public final boolean setInitial(final PlayerEntity player) {
        // Check if we need to initialize the value for the player, or if we should bail entirely
        if (!isValueInitialized(player)) {
            if (MinecraftForge.EVENT_BUS.post(new NeedInitializationEvent.Pre(this, player))) return false;
            initialize(player);
            MinecraftForge.EVENT_BUS.post(new NeedInitializationEvent.Post(this, player));
        }
        return true;
    }

    /**
     * Call after the value of a need changes from an external source; is automatically called
     * after adjustValue has fired.
     * @param player    The player whose values are updated
     * @param source    The source of the adjustment
     * @param prevValue The previous value of the need
     * @param newValue  The new value of the need
     */
    protected void sendUpdates(final PlayerEntity player, final IManipulator source, final double prevValue, final double newValue) {
        MinecraftForge.EVENT_BUS.post(new NeedAdjustmentEvent.Post(this, player, source, prevValue, newValue));

        // Deal with new level:
        final NeedLevel prevLevel = getLevel(prevValue);
        final NeedLevel newLevel = getLevel(newValue);
        if (!prevLevel.equals(newLevel)) {
            prevLevel.onExit(player);
            newLevel.onEnter(player);
            MinecraftForge.EVENT_BUS.post(new NeedLevelEvent.Changed(this, player, source, prevValue, newValue, prevLevel, newLevel));
        }

        // Make really, really, really sure we're on the server side before sending a packet to the player:
        if (!shouldSync() || !(player instanceof ServerPlayerEntity)) return;
        NetworkManager.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
            new NeedUpdatePacket(getName(), newValue, getMin(player), getMax(player))
        );
    }

    /**
     * Gets the manipulator list set for this need
     * @return  The manipulators
     */
    public List<IManipulator> getManipulators() {
        return manipulators;
    }

    /**
     * Gets the mixin list set for this need
     * @return  The mixins
     */
    public List<IMixin> getMixins() {
        return mixins;
    }

    /**
     * Gets the name of this need
     * @return  The display name of this need
     */
    public abstract String getName();

    /**
     * Gets the minimum value of this need; this will be called often
     * @return  The minimum value
     * @param player    The player to get the min for
     */
    public abstract double getMin(PlayerEntity player);

    /**
     * Gets the maximum value of this need; this will be called often
     * @return  The maximum value
     * @param player    The player to get the max for
     */
    public abstract double getMax(PlayerEntity player);

    /**
     * Initialize this need
     * @param player The player to initialize
     */
    public void initialize(final PlayerEntity player) {

    }

    /**
     * Get the current value of the need; this will be called often
     * @param player    The player to get the value for
     * @return          The current value
     */
    public abstract double getValue(PlayerEntity player);

    /**
     * Gets if the value is initialized for the given player; this will be called often
     * @param player    The player to check
     * @return          If the value is initialized for the player
     */
    public abstract boolean isValueInitialized(PlayerEntity player);

    /**
     * Gets the current level for the given player; this calls
     * getValue(player), so if you already have the value, you
     * should instead just use getLevel(value)
     * @param player The player to check for
     * @return       The level, or NeedLevel.UNDEFINED if there isn't one
     */
    @Nonnull
    public NeedLevel getLevel(final PlayerEntity player) {
        return getLevel(getValue(player));
    }

    /**
     * Gets the current level for the given value
     * @param value The value to check for
     * @return       The level, or NeedLevel.UNDEFINED if there isn't one
     */
    @Nonnull
    public NeedLevel getLevel(final double value) {
        final NeedLevel level = levels.get(value);
        return level != null ? level : NeedLevel.UNDEFINED;
    }

    /**
     * Gets the levels for this particular need
     * @return  The map of ranges to need levels
     */
    public Map<Range<Double>, NeedLevel> getLevels() {
        return levels.asMapOfRanges();
    }

    /**
     * Flags the need to be synced from the server to the client. This is generally set by a mixin,
     * but can also be set by a mod
     */
    public final void enableSyncing() {
        shouldSync = true;
        NeedRegistry.INSTANCE.registerForSync();
    }

    /**
     * Checks if the need should be synced
     * @return True if the need should sync
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public final boolean shouldSync() {
        return shouldSync;
    }

    /**
     * Sets the value of the need
     * @param player    The player to set the value for
     * @param newValue  The new value to set
     * @param adjustAmount  The amount to adjust by
     */
    protected abstract double setValue(final PlayerEntity player, final double newValue, final double adjustAmount);

    /**
     * Called when a player is respawning
     * @param event The event
     */
    private void onRespawned(final PlayerEvent.Clone event) {
        if (event.isCanceled() || !event.isWasDeath()) return;

        final NeedLevel level = getLevel(event.getPlayer());
        level.onRespawned(event.getPlayer(), event.getOriginal());
    }

    /**
     * Called when an entity has joined the world
     * @param event The event
     */
    private void onPlayerJoined(final PlayerLoggedInEvent event) {
        final NeedLevel level = getLevel(event.getPlayer());
        level.onPlayerJoined(event.getPlayer());
    }
}

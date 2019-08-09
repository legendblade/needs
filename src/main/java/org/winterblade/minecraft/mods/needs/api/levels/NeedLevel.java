package org.winterblade.minecraft.mods.needs.api.levels;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.actions.ILevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.IReappliedOnDeathLevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.TickingLevelAction;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(NeedLevel.Deserializer.class)
public class NeedLevel {
    /**
     * Denotes that the player isn't currently in a level
     */
    public static final NeedLevel UNDEFINED = new UndefinedLevel();

    /**
     * The display name of the level
     */
    @Expose
    protected String name;

    /**
     * The range the level will cover; by default this is built from the min/max properties
     */
    protected Range<Double> range;

    /**
     * A list of actions to fire when entering this level
     */
    @Expose
    protected List<ILevelAction> entryActions = Collections.emptyList();

    /**
     * A list of actions to fire while in this level
     */
    @Expose
    protected List<ILevelAction> continuousActions = Collections.emptyList();

    /**
     * A list of actions to fire when exiting this level
     */
    @Expose
    protected List<ILevelAction> exitActions = Collections.emptyList();

    protected List<IReappliedOnDeathLevelAction> reappliedActions;
    protected List<IReappliedOnDeathLevelAction> reappliedContinuousActions;
    protected Consumer<PlayerEntity> tickAction;

    protected Need parent;

    private static final WeakHashMap<PlayerEntity, NeedLevel> tickingPlayers = new WeakHashMap<>();
    private static boolean isListeningToTicks = false;

    /**
     * Gets the display name of this level
     * @return  The display name
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Gets the effective range of this level
     * @return  The range
     */
    @Nonnull
    public Range<Double> getRange() {
        return range;
    }

    /**
     * Gets if this level has any actions which require ticking
     * @return  True if so, false otherwise
     */
    public boolean hasTickingActions() {
        return tickAction != null;
    }

    /**
     * Fired when this level is created
     * @param parent The need the level is part of
     */
    public void onLoaded(final Need parent) {
        this.parent = parent;

        reappliedActions = new ArrayList<>();
        reappliedContinuousActions = new ArrayList<>();

        entryActions.forEach((ea) -> {
            ea.onLoaded(parent, this);
            if (!(ea instanceof IReappliedOnDeathLevelAction)) return;
            reappliedActions.add((IReappliedOnDeathLevelAction)ea);
        });

        exitActions.forEach((ea) -> ea.onLoaded(parent, this));

        // Build up our consumer
        for (final ILevelAction ca : continuousActions) {
            ca.onLoaded(parent, this);

            if ((ca instanceof IReappliedOnDeathLevelAction)) {
                reappliedContinuousActions.add((IReappliedOnDeathLevelAction) ca);
            }

            // I'm going to theorize that this is faster than forEach'ing the list:
            if (!(ca instanceof TickingLevelAction)) continue;
            final TickingLevelAction tla = (TickingLevelAction) ca;

            if (tickAction == null) tickAction = (p) -> tla.onContinuousTick(parent, this, p);
            else tickAction = tickAction.andThen((p) -> tla.onContinuousTick(parent, this, p));
        }

        if (tickAction != null && !isListeningToTicks) {
            MinecraftForge.EVENT_BUS.addListener(NeedLevel::onServerTick);
            isListeningToTicks = true;
        }

        // Freeze
        entryActions = ImmutableList.copyOf(entryActions);
        exitActions = ImmutableList.copyOf(exitActions);
        continuousActions = ImmutableList.copyOf(continuousActions);
        reappliedActions = ImmutableList.copyOf(reappliedActions);
        reappliedContinuousActions = ImmutableList.copyOf(reappliedContinuousActions);
    }

    /**
     * Called by the parent need when entering this level, after exit has been called on the previous level
     * @param player The player involved
     */
    public void onEnter(final PlayerEntity player) {
        entryActions.forEach((ea) -> ea.onEntered(parent, this, player));
        continuousActions.forEach((ea) -> ea.onContinuousStart(parent, this, player));
        if (hasTickingActions()) tickingPlayers.put(player, this);
    }

    /**
     * Called when exiting this level, called before entering the new level
     * @param player The player involved
     */
    public void onExit(final PlayerEntity player) {
        continuousActions.forEach((ea) -> ea.onContinuousEnd(parent, this, player));
        exitActions.forEach((ea) -> ea.onExited(parent, this, player));
        if (hasTickingActions()) tickingPlayers.remove(player);
    }

    /**
     * Called when the player joins a world
     * @param player The player
     */
    public void onPlayerJoined(final PlayerEntity player) {
        if (!hasTickingActions() || tickingPlayers.containsKey(player)) return;
        tickingPlayers.put(player, this);
    }

    /**
     * Called when ticking this level, per player
     * @param player The player involved
     */
    private void onTick(final PlayerEntity player) {
        if (!player.isAlive()) return;
        tickAction.accept(player);
    }

    /**
     * Called when the player respawns, in case actions need reapplied
     * @param player    The new player entity
     * @param oldPlayer The old player entity
     */
    public void onRespawned(final PlayerEntity player, final PlayerEntity oldPlayer) {
        reappliedActions.forEach((ea) -> ea.onRespawned(parent, this, player, oldPlayer));
        reappliedContinuousActions.forEach((ea) -> ea.onRespawnedWhenContinuous(parent, this, player, oldPlayer));

        if (hasTickingActions()) tickingPlayers.put(player, tickingPlayers.remove(oldPlayer));
    }

    /**
     * Called when ticking the server
     * @param event The event
     */
    private static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickingPlayers.forEach((p, v) -> v.onTick(p));
    }

    static class Deserializer implements JsonDeserializer<NeedLevel> {

        @Override
        public NeedLevel deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) throw new JsonParseException("Level must be an object.");
            final JsonObject obj = json.getAsJsonObject();

            final NeedLevel output = new NeedLevel();

            if (!obj.has("name")) throw new JsonParseException("Level must have a name.");
            output.name = obj.getAsJsonPrimitive("name").getAsString();

            // Deal with the ranges
            output.range = obj.has("range")
                ? RangeHelper.parseStringAsRange(obj.getAsJsonPrimitive("range").getAsString())
                : RangeHelper.parseObjectToRange(obj);

            // Actions:
            output.entryActions = deserializeActions(context, obj, "onEnter");
            output.exitActions = deserializeActions(context, obj, "onExit");
            output.continuousActions = deserializeActions(context, obj, "actions");

            return output;
        }

        private List<ILevelAction> deserializeActions(final JsonDeserializationContext context, final JsonObject obj, final String key) {
            if (!obj.has(key)) return Collections.emptyList();

            final JsonElement el = obj.get(key);

            if (el.isJsonObject()) {
                return Collections.singletonList(context.deserialize(el, ILevelAction.class));
            }

            if (el.isJsonArray()) {
                return Arrays.asList(context.deserialize(el, ILevelAction[].class));
            }

            throw new JsonParseException(key + " must be either a single object, or an array.");
        }
    }

    private static class UndefinedLevel extends NeedLevel {
        @Nonnull
        @Override
        public String getName() {
            return "Neutral";
        }

        @Nonnull
        @Override
        public Range<Double> getRange() {
            return Range.all();
        }

        @Override
        public void onLoaded(final Need parent) {

        }

        @Override
        public void onEnter(final PlayerEntity player) {

        }

        @Override
        public void onExit(final PlayerEntity player) {

        }

        @Override
        public void onRespawned(final PlayerEntity player, final PlayerEntity oldPlayer) {

        }

        @Override
        public boolean hasTickingActions() {
            return false;
        }

        @Override
        public void onPlayerJoined(final PlayerEntity player) {

        }
    }
}

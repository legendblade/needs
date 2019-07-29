package org.winterblade.minecraft.mods.needs.api;

import com.google.common.collect.*;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedInitializationEvent;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
@JsonAdapter(NeedRegistry.class)
public abstract class Need {
    @Expose
    private List<IManipulator> manipulators = Collections.emptyList();

    @Expose
    private List<IMixin> mixins = Collections.emptyList();

    @Expose
    @SerializedName("levels")
    private List<NeedLevel> levelList = Collections.emptyList();

    private RangeMap<Double, NeedLevel> levels = TreeRangeMap.create();

    /**
     * Determines if a need should be able to be declared more than once.
     * Override this anytime it makes sense to have multiple needs of the same class
     * @return  True if this can be declared more than once; false otherwise
     */
    public boolean allowMultiple() {
        return false;
    }

    public final void finalizeDeserialization() {
        // Freeze our manipulator list
        manipulators = ImmutableList.copyOf(manipulators);
        mixins = ImmutableList.copyOf(mixins);

        // Ensure our levels are properly defined and don't overlap:
        levelList.forEach((l) -> {
            RangeMap<Double, NeedLevel> subRange = levels.subRangeMap(l.getRange());
            if(0 < subRange.asMapOfRanges().size()) {
                throw new JsonParseException("This need has overlapping levels; ensure that min/max ranges do not overlap");
            }

            levels.put(l.getRange(), l);
        });
        levels = ImmutableRangeMap.copyOf(levels);

        onCreated();
    }

    public void onCreated() {

    }

    /**
     * Wrapper implementation to cast the entity to a player
     * @param entity    The entity to check
     * @param adjust    The amount to adjust by
     * @param source    The source of the adjustment
     */
    public final void adjustValue(Entity entity, double adjust, IManipulator source) {
        if (!(entity instanceof PlayerEntity)) return;
        adjustValue((PlayerEntity) entity, adjust, source);
    }

    /**
     * Adjust the value of this need for a given player
     * @param player      The player to adjust
     * @param adjust      The amount to adjust by
     * @param source      The source of the adjustment
     */
    public final void adjustValue(PlayerEntity player, double adjust, IManipulator source) {
        if (player == null || adjust == 0 || player.world.isRemote) return;

        // Check if we need to initialize the value for the player, or if we should bail entirely
        if (!isValueInitialized(player)) {
            if (MinecraftForge.EVENT_BUS.post(new NeedInitializationEvent.Pre(this, player))) return;
            initialize(player);
            MinecraftForge.EVENT_BUS.post(new NeedInitializationEvent.Post(this, player));
        }

        // Check if we shouldn't adjust the player at this moment
        if (MinecraftForge.EVENT_BUS.post(new NeedAdjustmentEvent.Pre(this, player, source))) return;

        // Get our current and clamped values:
        double current = getValue(player);
        NeedLevel prevLevel = getLevel(current);
        double newValue = Math.max(getMin(player), Math.min(getMax(player), current + adjust));

        // If the new value is the same as the current because we've hit the max/min, don't do anything
        if (newValue == current) return;

        // Finally, set the value and let our listeners know
        setValue(player, newValue);
        MinecraftForge.EVENT_BUS.post(new NeedAdjustmentEvent.Post(this, player, source, current, newValue));

        // Deal with new level:
        NeedLevel newLevel = getLevel(newValue);
        if (!prevLevel.equals(newLevel)) {
            prevLevel.onExit(player);
            newLevel.onEnter(player);
        }

        // TODO: Send updates to player
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
     * Gets the minimum value of this need
     * @return  The minimum value
     * @param player    The player to get the min for
     */
    public abstract double getMin(PlayerEntity player);

    /**
     * Gets the maximum value of this need
     * @return  The maximum value
     * @param player    The player to get the max for
     */
    public abstract double getMax(PlayerEntity player);

    /**
     * Initialize this need
     * @param player The player to initialize
     */
    public void initialize(PlayerEntity player) {

    }

    /**
     * Get the current value of the need
     * @param player    The player to get the value for
     * @return          The current value
     */
    public abstract double getValue(PlayerEntity player);

    /**
     * Gets if the value is initialized for the given player
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
    public NeedLevel getLevel(PlayerEntity player) {
        return getLevel(getValue(player));
    }

    /**
     * Gets the current level for the given value
     * @param value The value to check for
     * @return       The level, or NeedLevel.UNDEFINED if there isn't one
     */
    @Nonnull
    public NeedLevel getLevel(double value) {
        NeedLevel level = levels.get(value);
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
     * Sets the value of the need
     * @param player    The player to set the value for
     * @param newValue  The new value to set
     */
    protected abstract void setValue(PlayerEntity player, double newValue);
}

package org.winterblade.minecraft.mods.needs.api.levels;

import com.google.common.collect.Range;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.actions.ILevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.IReappliedOnDeathLevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.TickingLevelAction;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
@JsonAdapter(NeedLevel.Deserializer.class)
public class NeedLevel {
    public static final NeedLevel UNDEFINED = new UndefinedLevel();

    @Expose
    protected String name;

    protected Range<Double> range;

    @Expose
    protected List<ILevelAction> entryActions = Collections.emptyList();

    @Expose
    protected List<ILevelAction> continuousActions = Collections.emptyList();

    @Expose
    protected List<ILevelAction> exitActions = Collections.emptyList();

    @Expose
    protected List<IReappliedOnDeathLevelAction> reappliedActions = Collections.emptyList();

    @Expose
    protected List<IReappliedOnDeathLevelAction> reappliedContinuousActions = Collections.emptyList();

    protected Consumer<PlayerEntity> tickAction;

    protected Need parent;

    public String getName() {
        return name;
    }

    public Range<Double> getRange() {
        return range;
    }

    public void onCreated(Need parent) {
        this.parent = parent;

        entryActions.forEach((ea) -> {
            if (!(ea instanceof IReappliedOnDeathLevelAction)) return;
            reappliedActions.add((IReappliedOnDeathLevelAction)ea);
        });

        if(continuousActions.isEmpty()) return;

        // Build up our consumer
        for (ILevelAction ca : continuousActions) {
            if ((ca instanceof IReappliedOnDeathLevelAction)) {
                reappliedContinuousActions.add((IReappliedOnDeathLevelAction)ca);
            }

            if (!(ca instanceof TickingLevelAction)) continue;
            TickingLevelAction tla = (TickingLevelAction)ca;

            if (tickAction == null) tickAction = (p) -> tla.onContinuousTick(parent, this, p);
            else tickAction = tickAction.andThen((p) -> tla.onContinuousTick(parent, this, p));
        }
    }

    public void onEnter(PlayerEntity player) {
        entryActions.forEach((ea) -> ea.onEntered(parent, this, player));
        continuousActions.forEach((ea) -> ea.onContinuousStart(parent, this, player));
    }

    public void onExit(PlayerEntity player) {
        continuousActions.forEach((ea) -> ea.onContinuousEnd(parent, this, player));
        exitActions.forEach((ea) -> ea.onExited(parent, this, player));
    }

    private void onTick(PlayerEntity player) {
        tickAction.accept(player);
    }

    public void onRespawned(PlayerEvent.Clone event) {
        reappliedActions.forEach((ea) -> ea.onRespawned(parent, this, event.getEntityPlayer(), event.getOriginal()));
        reappliedContinuousActions.forEach((ea) -> ea.onRespawnedWhenContinuous(parent, this, event.getEntityPlayer(), event.getOriginal()));
    }

    static class Deserializer implements JsonDeserializer<NeedLevel> {

        @Override
        public NeedLevel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) throw new JsonParseException("Level must be an object.");
            JsonObject obj = json.getAsJsonObject();

            NeedLevel output = new NeedLevel();

            if (!obj.has("name")) throw new JsonParseException("Level must have a name.");
            output.name = obj.getAsJsonPrimitive("name").getAsString();

            // Deal with the ranges
            double min = Double.MIN_VALUE;
            double max = Double.MAX_VALUE;

            if (obj.has("min")) min = obj.getAsJsonPrimitive("min").getAsDouble();
            if (obj.has("max")) max = obj.getAsJsonPrimitive("max").getAsDouble();

            if (min == Double.MIN_VALUE && max == Double.MAX_VALUE) output.range = Range.all();
            else if(min == Double.MIN_VALUE) output.range = Range.lessThan(max);
            else if(max == Double.MAX_VALUE) output.range = Range.atLeast(min);
            else output.range = Range.closedOpen(min, max);

            if (output.range.isEmpty()) throw new JsonParseException("Invalid range specified: [" + min + "," + max + ")");

            // Actions:
            output.entryActions = deserializeActions(context, obj, "onEnter");
            output.exitActions = deserializeActions(context, obj, "onExit");
            output.continuousActions = deserializeActions(context, obj, "actions");

            return output;
        }

        private List<ILevelAction> deserializeActions(JsonDeserializationContext context, JsonObject obj, String key) {
            if (!obj.has(key)) return Collections.emptyList();

            JsonElement el = obj.get(key);

            if (el.isJsonObject()) {
                return Collections.singletonList(context.deserialize(el, LevelAction.class));
            }

            if (el.isJsonArray()) {
                return Arrays.asList(context.deserialize(el, LevelAction[].class));
            }

            throw new JsonParseException(key + " must be either a single object, or an array.");
        }
    }

    private static class UndefinedLevel extends NeedLevel {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public Range<Double> getRange() {
            return Range.all();
        }
    }
}

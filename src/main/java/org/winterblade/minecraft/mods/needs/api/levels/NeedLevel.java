package org.winterblade.minecraft.mods.needs.api.levels;

import com.google.common.collect.Range;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(NeedLevel.Deserializer.class)
public class NeedLevel {
    public static final NeedLevel UNDEFINED = new UndefinedLevel();

    @Expose
    protected String name;

    protected Range<Double> range;

    public String getName() {
        return name;
    }

    public Range<Double> getRange() {
        return range;
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

            return output;
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

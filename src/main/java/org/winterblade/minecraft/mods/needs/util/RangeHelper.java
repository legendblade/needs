package org.winterblade.minecraft.mods.needs.util;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

@SuppressWarnings("WeakerAccess")
public class RangeHelper {
    private RangeHelper() {}


    /**
     * Attempts to parse a string into a Range
     * @param rangeStr The input string
     * @return  The range
     */
    public static Range<Double> parseStringAsRange(String rangeStr) {
        rangeStr = rangeStr.trim();
        String withoutBoundTypes = rangeStr.replaceAll("[^\\-0-9.,]+", "");
        Range<Double> range;
        String[] split = withoutBoundTypes.split(",");

        if (split.length <= 0) return Range.all(); // Why?

        // Check if we have bounds; by default they will be '[x,y)'
        BoundType lowerBoundType = rangeStr.startsWith("(") ? BoundType.OPEN : BoundType.CLOSED;
        BoundType upperBoundType = rangeStr.endsWith("]") ? BoundType.CLOSED : BoundType.OPEN;

        // If we omitted one side:
        if (split.length <= 1) {
            if (withoutBoundTypes.endsWith(",")) return Range.downTo(Double.parseDouble(split[0]), lowerBoundType);
            if (withoutBoundTypes.startsWith(",")) return Range.upTo(Double.parseDouble(split[0]), upperBoundType);
            return Range.singleton(Double.parseDouble(split[0]));
        }

        // Because split won't return an empty side, none of these _should_ be possible, but check anyway:
        if (split[0].isEmpty() && split[1].isEmpty()) return Range.all();
        if (split[0].isEmpty()) return Range.upTo(Double.parseDouble(split[1]), upperBoundType);
        if (split[1].isEmpty()) return Range.downTo(Double.parseDouble(split[0]), lowerBoundType);

        return Range.range(
                Double.parseDouble(split[0]),
                lowerBoundType,
                Double.parseDouble(split[1]),
                upperBoundType
        );
    }

    /**
     * Parses the given min/max values as a range
     * @param min The min value
     * @param max The max value
     * @return  The new range
     */
    public static Range<Double> parseMinMaxAsRange(double min, double max) {
        if (min == Double.MIN_VALUE && max == Double.MAX_VALUE) return Range.all();
        if(min == Double.MIN_VALUE) return Range.lessThan(max);
        if(max == Double.MAX_VALUE) return Range.atLeast(min);
        return Range.closedOpen(min, max);
    }

    /**
     * Parses a JSON object with properties min/max into a Range
     * @param obj The object to parse
     * @return  The range
     */
    public static Range<Double> parseObjectToRange(JsonObject obj) throws JsonParseException {
        double min = Double.MIN_VALUE;
        double max = Double.MAX_VALUE;

        if (obj.has("min")) min = obj.getAsJsonPrimitive("min").getAsDouble();
        if (obj.has("max")) max = obj.getAsJsonPrimitive("max").getAsDouble();

        Range<Double> range = RangeHelper.parseMinMaxAsRange(min, max);
        if (range.isEmpty()) throw new JsonParseException("Invalid range specified: [" + min + "," + max + ")");

        return range;
    }
}

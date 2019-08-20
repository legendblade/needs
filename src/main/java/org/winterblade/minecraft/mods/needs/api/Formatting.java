package org.winterblade.minecraft.mods.needs.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Document(description = "Helps define how the need should be displayed to the user; all individual values are optional")
public class Formatting {
    private static final Map<Integer, String> si;
    static {
        final Map<Integer, String> temp = new HashMap<>();

        temp.put( 24, "Y");
        temp.put( 21, "Z");
        temp.put( 18, "E");
        temp.put( 15, "P");
        temp.put( 12, "T");
        temp.put(  9, "G");
        temp.put(  6, "M");
        temp.put(  3, "k");
        temp.put(  0, "");
        temp.put( -3, "m");
        temp.put( -6, "μ");
        temp.put( -9, "n");
        temp.put(-12, "p");
        temp.put(-15, "f");
        temp.put(-18, "a");
        temp.put(-21, "z");
        temp.put(-24, "y");

        si = ImmutableMap.copyOf(temp);
    }

    @Expose
    @Document(description = "An expression that will be used to modify the value prior to displaying it (in case you " +
            "want to multiply it, round it, square root things, etc).")
    @OptionalField(defaultValue = "None")
    private NeedExpressionContext expression;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @OptionalField(defaultValue = "0")
    @Document(description = "Sets the number of decimal places displayed; by default, this is 0")
    private int precision;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Should the value be converted to use the associated SI prefix; " +
            "see [https://en.wikipedia.org/wiki/Metric_prefix#List_of_SI_prefixes]")
    @OptionalField(defaultValue = "True")
    private boolean useSiPrefix;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "Unit of measure to be applied after the value (and SI prefix, if used)")
    @OptionalField(defaultValue = "None")
    private String unitOfMeasure;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "A string to prepend to the start of the value")
    @OptionalField(defaultValue = "None")
    private String prepend;

    @Expose
    @SuppressWarnings("FieldMayBeFinal")
    @Document(description = "A string to append to the end of the value")
    @OptionalField(defaultValue = "None")
    private String append;

    private boolean initialized;
    private String fmtString;
    private String precisionString;
    private double min;

    @SuppressWarnings("unused") // Deserialized by default GSON, needs default constructor to setup default values
    public Formatting() {
        precision = 0;
        useSiPrefix = true;
        unitOfMeasure = "";
        prepend = "";
        append = "";
    }

    /**
     * Initializes the format string and builds the expression
     */
    public void init() {
        precisionString = "%." + precision + "f";
        fmtString = prepend + " %s%s%s" + unitOfMeasure + append;
        if (expression != null) expression.build();

        min = Math.pow(10, -precision);

        initialized = true;
    }

    /**
     * Calculates the input value by the expression if any
     * @param input  The input
     * @param player The player to pass off to the expression (if any)
     * @return The calculated value
     */
    public double calculate(final double input, @Nonnull final PlayerEntity player) {
        if (expression == null) return input;

        expression.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> input);
        return expression.apply(player);
    }

    /**
     * Formats the input value
     * @param value  The input, is assumed to already be calculated
     * @return The formatted string
     */
    @Nonnull
    public String format(final double value) {
        // Make sure all our stuff is ready
        if (!initialized) init();

        // Get these conditions out of the way first...
        if (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY) {
            return String.format(fmtString, "", "\u221E", "");
        }

        if (value == 0) {
            return String.format(fmtString, "", "0", "");
        }

        // Do the shuffle
        int order = 0;
        double val = Math.abs(value);
        if (1000 <= val) {
            while (1000 <= val) {
                val /= 1000.0;
                order += 3;
            }
        } else if (0 < val){
            while(val < min) {
                val *= 1000.0;
                order -= 3;
            }
        }

        return String.format(
            fmtString,
            value <= 0 ? "-" : "",
            String.format(precisionString, val),
            si.getOrDefault(order, "\u209310^" + order)
        );
    }

    /**
     * Calculates the input value by the expression if any and then formats it
     * @param input  The input
     * @param player The player to pass off to the expression (if any)
     * @return The formatted string
     */
    public String calculateAndFormat(final double input, @Nonnull final PlayerEntity player) {
        return format(calculate(input, player));
    }
}

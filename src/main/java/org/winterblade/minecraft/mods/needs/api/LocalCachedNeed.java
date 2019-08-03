package org.winterblade.minecraft.mods.needs.api;

import com.google.common.collect.Range;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;

/**
 * This is a wrapper class for the local copy of a need on the client-side. Current, min, and max values should only
 * ever be read from this on the client, and not the need itself (which should be used on the server).
 */
public class LocalCachedNeed {
    private final WeakReference<Need> need;
    private double value;
    private double min;
    private double max;
    private final String name;

    private boolean hasLevels;
    private String level;
    private double lower;
    private double upper;

    public LocalCachedNeed(@Nonnull final Need need, final double value, final double min, final double max) {
        this.name = need.getName();
        this.need = new WeakReference<>(need);

        setValue(value);
        setMax(min);
        setMax(max);
    }

    /**
     * The local cached need; this need is not guaranteed to remain valid through configuration reloads
     * and the direct value should not be held onto itself.
     * @return  A weak reference to the need
     */
    public WeakReference<Need> getNeed() {
        return need;
    }

    /**
     * Gets the local cached value of the need
     * @return The local cached value
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the local cached value of the need
     * @param value The value
     */
    public void setValue(final double value) {
        this.value = value;

        final Need need = getNeed().get();
        if (need == null) return;

        // Cache this too:
        final NeedLevel level = need.getLevel(value);
        this.level = level.getName();

        if (level == NeedLevel.UNDEFINED) {
            // We need to get the next bound in another way
            final Map<Range<Double>, NeedLevel> levels = need.getLevels();

            if (0 < levels.size()) {
                final Iterator<Range<Double>> iter = levels.keySet().iterator();
                this.hasLevels = true;

                double low = Double.MIN_VALUE;
                double high = Double.MAX_VALUE;
                while (iter.hasNext()) {
                    final Range<Double> r = iter.next();
                    if (r.hasUpperBound() && r.upperEndpoint() <= value) low = r.upperEndpoint();

                    if (r.hasLowerBound() && value <= r.lowerEndpoint()) {
                        // Once we've found the upper bound, that's guaranteed to be the value we need
                        high = r.lowerEndpoint();
                        break;
                    }
                }

                this.lower = low;
                this.upper = high;
            } else {
                this.lower = Double.MIN_VALUE;
                this.upper = Double.MAX_VALUE;
                this.hasLevels = false;
            }
        } else {
            final Range<Double> range = level.getRange();
            this.lower = range.hasLowerBound() ? range.lowerEndpoint() : Double.MIN_VALUE;
            this.upper = range.hasUpperBound() ? range.upperEndpoint() : Double.MAX_VALUE;
            this.hasLevels = true;
        }
    }

    /**
     * Gets the name of this need
     * @return The name of the need
     */
    public String getName() {
        return name;
    }

    public double getMin() {
        return min;
    }

    public void setMin(final double min) {
        this.min = min;
    }

    public double getMax() {
       return max;
    }

    public void setMax(final double max) {
        this.max = max;
    }

    public String getLevel() {
        return level;
    }

    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }

    public boolean hasLevels() {
        return hasLevels;
    }
}

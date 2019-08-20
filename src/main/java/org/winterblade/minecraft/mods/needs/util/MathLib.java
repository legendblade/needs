package org.winterblade.minecraft.mods.needs.util;

import net.minecraft.util.math.MathHelper;

/**
 * This is my Math util library, there are many like it, but this one is mine.
 * And it's probably worse.
 */
public class MathLib {
    private MathLib() {} // No touchy

    /**
     * Clamp value between two pairs of min-max values
     * @param in   The value to clmap
     * @param min  The first minimum
     * @param max  The first maximum
     * @param min2 The second minimum
     * @param max2 The second maximum
     * @return The clamped value
     */
    public static double clamp(final double in, final double min, final double max, final double min2, final double max2) {
        return MathHelper.clamp(MathHelper.clamp(in, min2, max2), min, max);
    }

    /**
     * Return the minimum of three values
     * @param a The first value
     * @param b The second value
     * @param c The third value
     * @return The lowest value
     */
    public static double min3(final double a, final double b, final double c) {
        return Math.min(Math.min(a, c), b);
    }

    /**
     * Return the maximum of three values
     * @param a The first value
     * @param b The second value
     * @param c The third value
     * @return The greatest value
     */
    public static double max3(final double a, final double b, final double c) {
        return Math.max(Math.max(a, c), b);
    }
}

package org.winterblade.minecraft.mods.needs.util;

/**
 * This is my Math util library, there are many like it, but this one is mine.
 * And it's probably worse.
 */
public class MathLib {
    private MathLib() {} // No touchy

    /**
     * Clamp value between min and max
     * @param in   The value to clmap
     * @param min  The minimum
     * @param max  The maximum
     * @return The clamped value
     */
    public static double clamp(final double in, final double min, final double max) {
        return Math.max(min, Math.min(max, in));
    }

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
        return clamp(clamp(in, min2, max2), min, max);
    }
}

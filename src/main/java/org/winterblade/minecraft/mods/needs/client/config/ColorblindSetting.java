package org.winterblade.minecraft.mods.needs.client.config;

import org.winterblade.minecraft.mods.needs.util.ColorSet;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum ColorblindSetting {
    NORMAL("normal", ColorSet::setNormal, ColorSet::getNormal, 0x5fb2e6, (i,b) -> i),
    HIGH_CONTRAST("contrast", ColorSet::setContrast, ColorSet::getContrast, 0x00FFFF, Adjusters::Contrast),
    PROTANOPIA("protanopia", ColorSet::setProtanopia, ColorSet::getProtanopia, 0x5fb2e6, Adjusters::Protanopia),
    DEUTERANOPIA("deuteranopia", ColorSet::setDeuteranopia, ColorSet::getDeuteranopia, 0x5fb2e6, Adjusters::Deuteranopia),
    TRITANOPIA("tritanopia", ColorSet::setTritanopia, ColorSet::getTritanopia, 0x5fb2e6, Adjusters::Tritanopia),
    ACHROMATOPSIA("achromatopsia", ColorSet::setAchromatopsia, ColorSet::getAchromatopsia, 0x00FFFF, Adjusters::Achromatopsia),
    BLUE_CONE_MONOCHROMACY("blueCone", ColorSet::setBlueCone, ColorSet::getBlueCone, 0x00FFFF, Adjusters::BlueCone);

    private final String prop;
    private final BiConsumer<ColorSet, Integer> setter;
    private final Function<ColorSet, Optional<Integer>> getter;
    private final int defaultValue;
    private final BiFunction<Integer, Integer, Integer> adjuster;

    ColorblindSetting(final String prop, final BiConsumer<ColorSet, Integer> setter,
                      final Function<ColorSet, Optional<Integer>> getter, final int defaultValue,
                      final BiFunction<Integer, Integer, Integer> adjuster) {
        this.prop = prop;
        this.setter = setter;
        this.getter = getter;
        this.defaultValue = defaultValue;
        this.adjuster = adjuster;
    }

    public String getPropertyName() {
        return prop;
    }

    public void set(final ColorSet set, final int value) {
        setter.accept(set, value);
    }

    public int getAgainst(final ColorSet colorSet, final int background) {
        return getter.apply(colorSet).orElseGet(() -> adjuster.apply(colorSet.getNormal().orElse(defaultValue), background));
    }

    /*
     * TODO: These are some extremely weak implementations.
     */
    private static class Adjusters {
        static final int minContrast = 0x333333;
        static final int minContrastR = 0x003333;
        static final int minContrastG = 0x330033;
        static final int minContrastB = 0x333300;

        static final int maskContrast = 0xFFFFFF;
        static final int maskContrastR = 0x00FFFF;
        static final int maskContrastG = 0xFF00FF;
        static final int maskContrastB = 0xFFFF00;

        static int Contrast(final int input, final int background) {
            return adjust(input, background, minContrast, maskContrast);
        }

        static int Protanopia(final int input, final int background) {
            return adjust(input, background, minContrastR, maskContrastR);
        }

        static int Deuteranopia(final int input, final int background) {
            return adjust(input, background, minContrastG, maskContrastG);
        }

        static int Tritanopia(final int input, final int background) {
            return adjust(input, background, minContrastB, maskContrastB);
        }

        static int Achromatopsia(final int input, final int background) {
            return adjust(input, background, minContrast, maskContrast);
        }

        static int BlueCone(final int input, final int background) {
            return adjust(input, background, minContrastB, maskContrastB);
        }

        private static int adjust(final int input, final int background, final int minContrast, final int maskContrast) {
            final int outputMask = maskContrast | 0x444444;
            final int diff = Math.abs((background & maskContrast) - (input & maskContrast));
            if (minContrast <= diff) return input & outputMask;

            return (background < input)
                    ? (input + minContrast) < 0xFFFFFF
                        ? (input + minContrast) & outputMask
                        : (background - minContrast) & outputMask
                    : 0 < (input - minContrast)
                        ? (input - minContrast) & outputMask
                        : (background + minContrast) & outputMask;
        }
    }
}

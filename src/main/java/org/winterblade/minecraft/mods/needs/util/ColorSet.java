package org.winterblade.minecraft.mods.needs.util;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.client.config.ColorblindSetting;
import org.winterblade.minecraft.mods.needs.config.CoreConfig;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@JsonAdapter(ColorSet.Deserializer.class)
@Document(description = "Represents either a single color in the format of '#RRGGBB', '#RGB', or '0xRRGGBB', or a series " +
        "of colors used for each colorblindness setting. If a single color is specified, it will be assumed to be a normal " +
        "vision setting and other settings will attempt to adjust dynamically based on (very loose) algorithms compared to " +
        "the background it's rendered against.\n\n" +
        "Properties are: `normal`, `contrast`, `protanopia`, `deuteranopia`, `tritanopia`, `achromatopsia`, and `blueCone`.")
public class ColorSet {
    private Optional<Integer> normal;
    private Optional<Integer> contrast;
    private Optional<Integer> protanopia;
    private Optional<Integer> deuteranopia;
    private Optional<Integer> tritanopia;
    private Optional<Integer> achromatopsia;
    private Optional<Integer> blueCone;

    @SuppressWarnings("WeakerAccess")
    public ColorSet() {
        normal = contrast = protanopia = deuteranopia = tritanopia = achromatopsia = blueCone = Optional.empty();
    }

    @SuppressWarnings("WeakerAccess")
    public ColorSet(final int normal) {
        this();
        setNormal(normal);
    }

    public Optional<Integer> getNormal() {
        return normal;
    }

    public void setNormal(final int normal) {
        this.normal = Optional.of(normal);
    }

    public Optional<Integer> getContrast() {
        return contrast;
    }

    public void setContrast(final int contrast) {
        this.contrast = Optional.of(contrast);
    }

    public Optional<Integer> getProtanopia() {
        return protanopia;
    }

    public void setProtanopia(final int protanopia) {
        this.protanopia = Optional.of(protanopia);
    }

    public Optional<Integer> getDeuteranopia() {
        return deuteranopia;
    }

    public void setDeuteranopia(final int deuteranopia) {
        this.deuteranopia = Optional.of(deuteranopia);
    }

    public Optional<Integer> getTritanopia() {
        return tritanopia;
    }

    public void setTritanopia(final int tritanopia) {
        this.tritanopia = Optional.of(tritanopia);
    }

    public Optional<Integer> getAchromatopsia() {
        return achromatopsia;
    }

    public void setAchromatopsia(final int achromatopsia) {
        this.achromatopsia = Optional.of(achromatopsia);
    }

    public Optional<Integer> getBlueCone() {
        return blueCone;
    }

    public void setBlueCone(final int blueCone) {
        this.blueCone = Optional.of(blueCone);
    }

    public int getAgainst(final int background) {
        final ColorblindSetting setting = CoreConfig.CLIENT.colorblindess.get();
        return setting.getAgainst(this, background);
    }

    public int get() {
        return getAgainst(0x888888);
    }

    public static class Deserializer implements JsonDeserializer<ColorSet> {
        @Override
        public ColorSet deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) return new ColorSet(getColor(json));

            if (!json.isJsonObject()) throw new JsonParseException("Unknown color format: " + json.toString());

            final ColorSet output = new ColorSet();

            final JsonObject obj = json.getAsJsonObject();
            Arrays.stream(ColorblindSetting.values())
                    .filter((s) -> obj.has(s.getPropertyName()))
                    .forEach((s) -> s.set(output, getColor(obj.getAsJsonPrimitive(s.getPropertyName()))));

            return output;
        }

        private static int getColor(final JsonElement json) throws JsonParseException {
            if (!json.isJsonPrimitive()) throw new JsonParseException("Unknown color format: " + json.toString());
            final JsonPrimitive jp = json.getAsJsonPrimitive();

            if (jp.isNumber()) jp.getAsInt();
            if (!jp.isString()) throw new JsonParseException("Unknown color format: " + jp.toString());

            String colorStr = jp.getAsString();
            if (colorStr.isEmpty()) throw new JsonParseException("Color cannot be an empty string.");

            if (colorStr.startsWith("0x") || colorStr.startsWith("#")) {
                colorStr = colorStr.replaceFirst("0x|#","");

                if (colorStr.length() != 3 && colorStr.length() != 6) throw new JsonParseException("Unknown color format: " + jp.toString());

                if (colorStr.length() == 3) {
                    colorStr = String.valueOf(
                            colorStr.charAt(0)) + colorStr.charAt(0) +
                            colorStr.charAt(1) + colorStr.charAt(1) +
                            colorStr.charAt(2) + colorStr.charAt(2);
                }
                return Integer.parseInt(colorStr, 16);
            }

            // I'd convert it from a Minecraft color, but Mojang decided getColor is only available on the client. :|
            throw new JsonParseException("Unknown color format: " + jp.toString());
        }
    }
}

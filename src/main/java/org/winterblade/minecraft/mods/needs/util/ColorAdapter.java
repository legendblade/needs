package org.winterblade.minecraft.mods.needs.util;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ColorAdapter implements JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive jp = json.getAsJsonPrimitive();

            if (jp.isNumber()) return jp.getAsInt();
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

        // TODO: Parse an RGB object format?
        throw new JsonParseException("Unknown color format: " + json.toString());
    }
}

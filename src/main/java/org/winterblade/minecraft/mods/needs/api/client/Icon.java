package org.winterblade.minecraft.mods.needs.api.client;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.util.ResourceLocation;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.client.gui.ExpressionPositionedTexture;

import java.lang.reflect.Type;

@Document(description = "An icon to use on the UI; if specifying no other properties, you may pass this in as a string " +
        "in the same format as the `icon` field")
@JsonAdapter(Icon.Deserializer.class)
public class Icon {
    private static final ExpressionPositionedTexture GENERIC_ICON =
            new ExpressionPositionedTexture(new ResourceLocation(NeedsMod.MODID, "gui/generic_need.png"), 34, 34);

    @Expose
    @Document(description = "A path to an icon to use.")
    @SuppressWarnings("FieldMayBeFinal")
    private TextureResource icon;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The width of the icon; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int width = 32;

    @Expose
    @OptionalField(defaultValue = "32")
    @Document(description = "The height of the icon; this defaults to 32px")
    @SuppressWarnings("FieldMayBeFinal")
    private int height = 32;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the X coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private NeedExpressionContext x = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the Y coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private NeedExpressionContext y = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "(32-width)/2")
    @Document(description = "Adjust this if you want to adjust the X offset of the icon in the UI; by default, it " +
            "will be `(32-width)/2`")
    @SuppressWarnings("FieldMayBeFinal")
    private int offsetX = Integer.MIN_VALUE;

    @Expose
    @OptionalField(defaultValue = "(32-height)/2")
    @Document(description = "Adjust this if you want to adjust the Y offset of the icon in the UI; by default, it " +
            "will be `(32-height)/2`")
    @SuppressWarnings("FieldMayBeFinal")
    private int offsetY = Integer.MIN_VALUE;

    @Expose
    @OptionalField(defaultValue = "Icon Width")
    @Document(description = "The width of the icon texture; if using a texture sheet, this should be the width of the sheet")
    @SuppressWarnings("FieldMayBeFinal")
    private int textureWidth = Integer.MIN_VALUE;

    @Expose
    @OptionalField(defaultValue = "Icon Height")
    @Document(description = "The height of the icon texture; if using a texture sheet, this should be the height of the sheet")
    @SuppressWarnings("FieldMayBeFinal")
    private int textureHeight = Integer.MIN_VALUE;

    public void validate() throws IllegalArgumentException {
        if (x == null) throw new IllegalArgumentException("x cannot be null.");
        if (y == null) throw new IllegalArgumentException("y cannot be null.");
    }

    private ExpressionPositionedTexture texture;

    public void onLoaded() {
        if (offsetX == Integer.MIN_VALUE) offsetX = (32 - width) / 2;
        if (offsetY == Integer.MIN_VALUE) offsetY = (32 - height) / 2;
        if (textureWidth == Integer.MIN_VALUE) textureWidth = width;
        if (textureHeight == Integer.MIN_VALUE) textureHeight = height;

        texture = icon != null
                ? new ExpressionPositionedTexture(
                icon,
                textureWidth,
                textureHeight,
                x,
                y,
                width,
                height
        ) : GENERIC_ICON;

        // If we have any dependent needs, make sure they're synced.
        x.syncAll();
        y.syncAll();
    }

    public ExpressionPositionedTexture getTexture() {
        return texture != null ? texture : GENERIC_ICON;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public boolean isNotGeneric() {
        return icon != null;
    }

    protected static class Deserializer implements JsonDeserializer<Icon> {

        @Override
        public Icon deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            // I don't think this happens?
            if (json.isJsonNull()) return null;

            final Icon out = new Icon();
            if (json.isJsonPrimitive()) {
                out.icon = context.deserialize(json, TextureResource.class);
                return out;
            }

            if (json.isJsonArray()) throw new JsonParseException("Icon must be either a string or an object.");

            final JsonObject obj = json.getAsJsonObject();
            if (!obj.has("icon")) throw new JsonParseException("Icon must have icon path.");

            out.icon = context.deserialize(obj.get("icon"), TextureResource.class);
            out.width = context.deserialize(obj.get("width"), Integer.class);
            out.height = context.deserialize(obj.get("height"), Integer.class);
            out.x = context.deserialize(obj.get("x"), NeedExpressionContext.class);
            out.y = context.deserialize(obj.get("y"), NeedExpressionContext.class);
            out.offsetX = context.deserialize(obj.get("offsetX"), Integer.class);
            out.offsetY = context.deserialize(obj.get("offsetY"), Integer.class);
            out.textureWidth = context.deserialize(obj.get("textureWidth"), Integer.class);
            out.textureHeight = context.deserialize(obj.get("textureHeight"), Integer.class);

            return out;
        }
    }
}

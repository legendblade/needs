package org.winterblade.minecraft.mods.needs.api.client.gui;

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
    private NeedExpressionContext textureX = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "If your icon path points to a texture sheet, instead of a single icon, use this to " +
            "specify the Y coordinate of the icon on the texture sheet.")
    @SuppressWarnings("FieldMayBeFinal")
    private NeedExpressionContext textureY = ExpressionContext.makeConstant(new NeedExpressionContext(), 0);

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "Adjust this if you want to adjust the X offset of the icon in the UI.")
    @SuppressWarnings("FieldMayBeFinal")
    private int x = 0;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "Adjust this if you want to adjust the Y offset of the icon in the UI.")
    @SuppressWarnings("FieldMayBeFinal")
    private int y = 0;

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
        if (textureX == null) throw new IllegalArgumentException("textureX cannot be null.");
        if (textureY == null) throw new IllegalArgumentException("textureY cannot be null.");
    }

    private ExpressionPositionedTexture texture;

    public void onLoaded() {
        if (textureWidth == Integer.MIN_VALUE) textureWidth = width;
        if (textureHeight == Integer.MIN_VALUE) textureHeight = height;

        textureX.build();
        textureY.build();

        texture = icon != null
            ? new ExpressionPositionedTexture(
                icon,
                textureWidth,
                textureHeight,
                textureX,
                textureY,
                width,
                height
        ) : GENERIC_ICON;
    }

    public ExpressionPositionedTexture getTexture() {
        return texture != null ? texture : GENERIC_ICON;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
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

            if (obj.has("icon")) out.icon = context.deserialize(obj.get("icon"), TextureResource.class);
            if (obj.has("width")) out.width = context.deserialize(obj.get("width"), Integer.class);
            if (obj.has("height")) out.height = context.deserialize(obj.get("height"), Integer.class);
            if (obj.has("textureX")) out.textureX = context.deserialize(obj.get("textureX"), NeedExpressionContext.class);
            if (obj.has("textureY")) out.textureY = context.deserialize(obj.get("textureY"), NeedExpressionContext.class);
            if (obj.has("x")) out.x = context.deserialize(obj.get("x"), Integer.class);
            if (obj.has("y")) out.y = context.deserialize(obj.get("y"), Integer.class);
            if (obj.has("textureWidth")) out.textureWidth = context.deserialize(obj.get("textureWidth"), Integer.class);
            if (obj.has("textureHeight")) out.textureHeight = context.deserialize(obj.get("textureHeight"), Integer.class);

            return out;
        }
    }
}

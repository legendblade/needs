package org.winterblade.minecraft.mods.needs.api.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.util.ResourceLocation;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.util.DynamicTextureUtil;

import java.lang.reflect.Type;
import java.util.function.Supplier;

@Document(description = "A path to a texture, which can be:\n\n- One of the builtin textures, which can be specified " +
        "with just the name (e.g. `'w_sword001'`)\n- A texture from another mod by using the full path " +
        "(e.g. `'minecraft:textures/item/iron_helmet'`)\n- A local image placed in the `'config/needs/textures'` " +
        "directory (e.g. `'file:path/to/image.png'`")
@JsonAdapter(TextureResource.Deserializer.class)
public class TextureResource implements Supplier<ResourceLocation> {
    private final boolean isBuiltin;

    private Supplier<ResourceLocation> locationSupplier;
    private ResourceLocation location;

    public TextureResource(final Supplier<ResourceLocation> res) {
        this(res, false);
    }

    public TextureResource(final Supplier<ResourceLocation> res, final boolean isBuiltin) {
        locationSupplier = res;
        this.isBuiltin = isBuiltin;
    }

    public TextureResource(final ResourceLocation res, final boolean isBuiltin) {
        location = res;
        this.isBuiltin = isBuiltin;
    }

    public static TextureResource getTextureLocation(String path) {
        if (path != null && path.startsWith("file:")) {
            return DynamicTextureUtil.getDynamicTexture(path);
        } else if (path != null && !path.isEmpty()) {
            if (!path.contains(".")) path += ".png";
            final String[] split = path.split(":");

            if (split.length <= 0) return null;
            else if (split.length <= 1) {
                return new TextureResource(new ResourceLocation(NeedsMod.MODID, "textures/gui/needs/" + path), true);
            } else {
                return new TextureResource(new ResourceLocation(split[0], split[1]), split[0].equals(NeedsMod.MODID));
            }
        }

        return null;
    }

    @Override
    public ResourceLocation get() {
        if (location != null) return location;
        location = locationSupplier.get();
        return location;
    }

    protected static class Deserializer implements JsonDeserializer<TextureResource> {
        @Override
        public TextureResource deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return getTextureLocation(json.getAsString());
        }
    }
}

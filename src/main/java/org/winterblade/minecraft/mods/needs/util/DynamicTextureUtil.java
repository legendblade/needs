package org.winterblade.minecraft.mods.needs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.client.TextureResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DynamicTextureUtil {
    private static final Path root = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "needs", "textures");

    /**
     * Gets a supplier for a dynamic texture located in the config/needs/textures/ directory.
     *
     * This is wrapped in a supplier due to the fact that it must only be unwrapped when the GL context is in
     * a proper state - generally when it's already rendering textures.
     *
     * This method is safe to call, but will always return null, when on the {@link DedicatedServer}.
     * @param icon The icon path
     * @return A supplier for a dynamic resource location or null if one could not be obtained
     */
    @Nullable
    public static TextureResource getDynamicTexture(final String icon) {
        return DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> {
            final Path path = Paths.get(root.toString(), icon.substring(5)).normalize();

            // Hopefully this will sandbox well enough, otherwise: look into the security manager.
            // Really this would only impact local resources anyway, but. #Safety?
            if (root.relativize(path).startsWith("..")) {
                NeedsMod.LOGGER.error("Cannot access texture file outside of config/needs/textures/");
                return null;
            }

            final NativeImage logo;

            try {
                final InputStream logoResource = Files.newInputStream(path);
                logo = NativeImage.read(logoResource);
            } catch (final IOException e) {
                NeedsMod.LOGGER.warn("Unable to access image " + icon);
                return null;
            }

            final String name = icon.substring(5).replaceAll("[/.]", "_");
            return new TextureResource(() -> Minecraft
                    .getInstance()
                    .getTextureManager()
                    .getDynamicTextureLocation(name, new DynamicTexture(logo)));
        });
    }
}

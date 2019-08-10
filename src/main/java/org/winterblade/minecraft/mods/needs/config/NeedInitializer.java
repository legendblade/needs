package org.winterblade.minecraft.mods.needs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.winterblade.minecraft.mods.needs.CoreRegistration;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NeedInitializer {
    public static final NeedInitializer INSTANCE = new NeedInitializer();

    private final GsonBuilder builder = new GsonBuilder();
    private Gson gson;

    private NeedInitializer() {
        builder.excludeFieldsWithoutExposeAnnotation();
    }

    public void setup(@SuppressWarnings("unused") final FMLCommonSetupEvent event) {
        CoreRegistration.register();

        try {
            final Path jsonDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "needs");

            if (!Files.isDirectory(jsonDir)) {
                try {
                    Files.createDirectory(jsonDir);
                } catch (final IOException e) {
                    NeedsMod.LOGGER.warn("Unable to create default config directory.");
                }
                return;
            }

            final Stream<Path> files = Files.walk(jsonDir)
                    .filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".json"));

            for (final Path file : (Iterable<Path>)files::iterator) {
                try {
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    final DigestInputStream dis = new DigestInputStream(Files.newInputStream(file), md);
                    final InputStreamReader reader = new InputStreamReader(dis);

                    // If we're on the client, don't bother with the extra step to store content
                    DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
                        final Need need = GetGson().fromJson(reader, Need.class);
                        register(file, md, need, null);
                    });

                    // Otherwise, do.
                    DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> {
                        final BufferedReader buf = new BufferedReader(reader);
                        final String content = buf.lines().collect(Collectors.joining());

                        final Need need = GetGson().fromJson(content, Need.class);
                        register(file, md, need, content);
                    });
                } catch (final Exception e) {
                    NeedsMod.LOGGER.warn("Error reading needs file '" + file.toString() + "': " + e.toString());
                }
            }
        } catch (final Exception e) {
            NeedsMod.LOGGER.warn("Error reading needs directory: " + e.toString());
        }

        NeedRegistry.INSTANCE.validateDependencies();
    }

    /**
     * Registers the need after deserialization
     * @param file    The file the need was loaded from
     * @param md      The message digest
     * @param need    The need itself
     * @param content The file content if on the server side
     */
    private void register(final Path file, final MessageDigest md, final Need need, @Nullable final String content) {
        if (need instanceof CustomNeed && (need.getName() == null || need.getName().isEmpty())) {
            ((CustomNeed)need).setName(FilenameUtils.getBaseName(file.toString()));
        }

        if (!NeedRegistry.INSTANCE.isValid(need)) {
            throw new IllegalArgumentException("This need duplicates another need");
        }

        need.beginValidate();
        NeedRegistry.INSTANCE.register(need, need.getName(), md.digest(), content);
    }

    private Gson GetGson() {
        if (gson == null) gson = builder.create();

        return gson;
    }
}

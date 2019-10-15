package org.winterblade.minecraft.mods.needs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.winterblade.minecraft.mods.needs.CoreRegistration;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.needs.NeedConfigSources;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
                final String filename = file.toString();
                try {
                    final NeedDefinition result = parseStream(Files.newInputStream(file));
                    if (result == null) continue;

                    final Need need = result.getNeed();
                    if (need instanceof CustomNeed && (need.getName() == null || need.getName().isEmpty())) {
                        ((CustomNeed) need).setName(FilenameUtils.getBaseName(filename));
                    }

                    NeedRegistry.INSTANCE.register(NeedConfigSources.CONFIG, result);
                } catch (final Exception e) {
                    NeedsMod.LOGGER.warn("Error reading needs file '" + filename + "': " + e.toString());
                }
            }
        } catch (final Exception e) {
            NeedsMod.LOGGER.warn("Error reading needs directory: " + e.toString());
        }
    }

    /**
     * Parses a need stream
     * @param inputStream The input stream to parse
     */
    public NeedDefinition parseStream(final InputStream inputStream) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final DigestInputStream dis = new DigestInputStream(inputStream, md);
            final InputStreamReader reader = new InputStreamReader(dis);

            // If we're on the client, don't bother with the extra step to store content
            return DistExecutor.runForDist(
                    () -> () -> new NeedDefinition(GetGson().fromJson(reader, Need.class), "", md.digest()),
                    () -> () -> {
                        final BufferedReader buf = new BufferedReader(reader);
                        final String content = buf.lines().collect(Collectors.joining());

                        return new NeedDefinition(GetGson().fromJson(content, Need.class), content, md.digest());
                    });
        } catch (final Exception ex) {
            NeedsMod.LOGGER.error("Error parsing need: " + ex.toString());
            return null;
        }
    }

    private Gson GetGson() {
        if (gson == null) gson = builder.create();

        return gson;
    }

}

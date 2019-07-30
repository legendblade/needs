package org.winterblade.minecraft.mods.needs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.winterblade.minecraft.mods.needs.CoreRegistration;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

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
            Path jsonDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "needs");

            if (!Files.isDirectory(jsonDir)) {
                try {
                    Files.createDirectory(jsonDir);
                } catch (IOException e) {
                    NeedsMod.LOGGER.warn("Unable to create default config directory.");
                }
                return;
            }

            Stream<Path> files = Files.walk(jsonDir)
                    .filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".json"));

            for (Path file : (Iterable<Path>)files::iterator) {
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    DigestInputStream dis = new DigestInputStream(Files.newInputStream(file), md);
                    InputStreamReader reader = new InputStreamReader(dis);

                    Need need = GetGson().fromJson(reader, Need.class);

                    if (need instanceof CustomNeed && (need.getName()== null || need.getName().isEmpty())) {
                        ((CustomNeed)need).setName(FilenameUtils.getBaseName(file.toString()));
                    }

                    if (!NeedRegistry.INSTANCE.isValid(need)) {
                        throw new IllegalArgumentException("This need duplicates another need");
                    }

                    need.finalizeDeserialization();
                    NeedRegistry.INSTANCE.register(need, need.getName(), md.digest());
                } catch (Exception e) {
                    NeedsMod.LOGGER.warn("Error reading needs file '" + file.toString() + "': " + e.toString());
                }
            }
        } catch (Exception e) {
            NeedsMod.LOGGER.warn("Error reading needs directory: " + e.toString());
        }

        NeedRegistry.INSTANCE.validateDependencies();
    }

    private Gson GetGson() {
        if (gson == null) gson = builder.create();

        return gson;
    }
}

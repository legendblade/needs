package org.winterblade.minecraft.mods.needs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.winterblade.minecraft.mods.needs.Need;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.PerHourManipulator;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class NeedInitializer {
    public static final NeedInitializer INSTANCE = new NeedInitializer();

    private final GsonBuilder builder = new GsonBuilder();
    private Gson gson;

    private NeedInitializer() {
        builder.excludeFieldsWithoutExposeAnnotation();
    }

    public void setup(@SuppressWarnings("unused") final FMLCommonSetupEvent event) {
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
                    Need need = GetGson().fromJson(new FileReader(file.toString()), Need.class);

                    if (need.name == null || need.name.isEmpty()) need.name = FilenameUtils.getBaseName(file.toString());

                    need.OnCreated();
                    need.GetManipulators().forEach((m) -> m.OnCreated(need));
                } catch (Exception e) {
                    NeedsMod.LOGGER.warn("Error reading needs file '" + file.toString() + "': " + e.toString());
                }
            }
        } catch (Exception e) {
            NeedsMod.LOGGER.warn("Error reading needs directory: " + e.toString());
        }
    }

    private Gson GetGson() {
        if (gson == null) gson = builder.create();
        return gson;
    }
}

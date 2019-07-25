package org.winterblade.minecraft.mods.needs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.OnDeathManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.PerHourManipulator;
import org.winterblade.minecraft.mods.needs.mixins.ChatMixin;
import org.winterblade.minecraft.mods.needs.mixins.ScoreboardMixin;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

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

        // Need classes
        NeedRegistry.INSTANCE.register("custom", CustomNeed.class);

        // Manipulators
        ManipulatorRegistry.INSTANCE.register("itemUsed", ItemUsedManipulator.class);
        ManipulatorRegistry.INSTANCE.register("perHour", PerHourManipulator.class);
        ManipulatorRegistry.INSTANCE.register("onDeath", OnDeathManipulator.class);

        // Mixins
        MixinRegistry.INSTANCE.register("scoreboard", ScoreboardMixin.class);
        MixinRegistry.INSTANCE.register("chat", ChatMixin.class);
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

                    if (need instanceof CustomNeed && (need.getName()== null || need.getName().isEmpty())) {
                        ((CustomNeed)need).setName(FilenameUtils.getBaseName(file.toString()));
                    }

                    need.finalizeDeserialization();

                    need.getManipulators().forEach((m) -> {
                            m.onCreated(need);
                            MinecraftForge.EVENT_BUS.register(m);
                    });

                    need.getMixins().forEach((m) -> {
                        m.onCreated(need);
                        MinecraftForge.EVENT_BUS.register(m);
                    });
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

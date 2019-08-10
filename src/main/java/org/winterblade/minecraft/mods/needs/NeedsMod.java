package org.winterblade.minecraft.mods.needs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mariuszgromada.math.mxparser.mXparser;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.ClientRegistration;
import org.winterblade.minecraft.mods.needs.config.NeedInitializer;
import org.winterblade.minecraft.mods.needs.documentation.DocumentationBuilder;

@Mod(NeedsMod.MODID)
public class NeedsMod
{
    public static final String MODID = "needswantsdesires";
    public static final String VERSION = "0.0.0.1";

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public NeedsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(NeedInitializer.INSTANCE::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::imc);
    }

    @SuppressWarnings("unused")
    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Needs, Wants, Desires uses mXparser for expression parsing. mXparser is released under the following license:\n" + mXparser.getLicense());

        MinecraftForge.EVENT_BUS.addListener(NeedRegistry.INSTANCE::onLogin);
        MinecraftForge.EVENT_BUS.addListener(NeedRegistry.INSTANCE::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(NeedRegistry.INSTANCE::onServerStopped);
    }

    private void setupClient(final FMLClientSetupEvent event) {
        ClientRegistration.register();
    }

    private void imc(final InterModProcessEvent event) {
        DocumentationBuilder.buildDocumentation();
    }
}

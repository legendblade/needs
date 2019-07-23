package org.winterblade.minecraft.mods.needs;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.winterblade.minecraft.mods.needs.config.NeedInitializer;

@Mod("needswantsdesires")
public class NeedsMod
{
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public NeedsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(NeedInitializer.INSTANCE::setup);
    }

    @SuppressWarnings("unused")
    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Hi from pre-init?");
    }
}

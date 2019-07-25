package org.winterblade.minecraft.mods.needs;

import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.OnDeathManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.PerHourManipulator;
import org.winterblade.minecraft.mods.needs.mixins.ChatMixin;
import org.winterblade.minecraft.mods.needs.mixins.ScoreboardMixin;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;
import org.winterblade.minecraft.mods.needs.needs.vanilla.FoodNeed;

public class CoreRegistration {
    public static void register() {
        registerNeeds();
        registerManipulators();
        registerMixins();
    }

    private static void registerNeeds() {
        // Need classes
        NeedRegistry.INSTANCE.register("custom", CustomNeed.class);
        NeedRegistry.INSTANCE.register("food", FoodNeed.class);
    }

    private static void registerMixins() {
        // Mixins
        MixinRegistry.INSTANCE.register("scoreboard", ScoreboardMixin.class);
        MixinRegistry.INSTANCE.register("chat", ChatMixin.class);
    }

    private static void registerManipulators() {
        // Manipulators
        ManipulatorRegistry.INSTANCE.register("itemUsed", ItemUsedManipulator.class);
        ManipulatorRegistry.INSTANCE.register("perHour", PerHourManipulator.class);
        ManipulatorRegistry.INSTANCE.register("onDeath", OnDeathManipulator.class);
    }
}

package org.winterblade.minecraft.mods.needs;

import org.winterblade.minecraft.mods.needs.actions.PotionEffectLevelAction;
import org.winterblade.minecraft.mods.needs.actions.TestingTickLevelAction;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.OnDeathManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.OnNeedChangedManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.PerHourManipulator;
import org.winterblade.minecraft.mods.needs.mixins.ChatLevelMixin;
import org.winterblade.minecraft.mods.needs.mixins.ChatMixin;
import org.winterblade.minecraft.mods.needs.mixins.ScoreboardMixin;
import org.winterblade.minecraft.mods.needs.mixins.UiMixin;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;
import org.winterblade.minecraft.mods.needs.needs.vanilla.FoodNeed;
import org.winterblade.minecraft.mods.needs.needs.vanilla.HealthNeed;
import org.winterblade.minecraft.mods.needs.network.NetworkManager;

public class CoreRegistration {
    public static void register() {
        registerNeeds();
        registerManipulators();
        registerMixins();
        registerActions();
        registerDebug();

        NetworkManager.register();
    }

    private static void registerNeeds() {
        // Need classes
        NeedRegistry.INSTANCE.register("custom", CustomNeed.class);
        NeedRegistry.INSTANCE.register("food", FoodNeed.class, "hunger");
        NeedRegistry.INSTANCE.register("health", HealthNeed.class, "hp");
    }

    private static void registerMixins() {
        // Mixins
        MixinRegistry.INSTANCE.register("scoreboard", ScoreboardMixin.class, "score");
        MixinRegistry.INSTANCE.register("chatOnChanged", ChatMixin.class);
        MixinRegistry.INSTANCE.register("chatOnLevel", ChatLevelMixin.class, "chat");
        MixinRegistry.INSTANCE.register("ui", UiMixin.class, "gui");
    }

    private static void registerManipulators() {
        // Manipulators
        ManipulatorRegistry.INSTANCE.register("itemUsed", ItemUsedManipulator.class);
        ManipulatorRegistry.INSTANCE.register("perHour", PerHourManipulator.class);
        ManipulatorRegistry.INSTANCE.register("onDeath", OnDeathManipulator.class);
        ManipulatorRegistry.INSTANCE.register("onNeedChanged", OnNeedChangedManipulator.class);
    }

    private static void registerActions() {
        // Actions
        LevelActionRegistry.INSTANCE.register("potionEffect", PotionEffectLevelAction.class);
    }

    private static void registerDebug() {
        LevelActionRegistry.INSTANCE.register("tickDebug", TestingTickLevelAction.class);
    }
}

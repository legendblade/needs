package org.winterblade.minecraft.mods.needs;

import net.minecraftforge.common.capabilities.CapabilityManager;
import org.winterblade.minecraft.mods.needs.actions.AdjustNeedLevelAction;
import org.winterblade.minecraft.mods.needs.actions.PotionEffectLevelAction;
import org.winterblade.minecraft.mods.needs.actions.TestingTickLevelAction;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.manipulators.*;
import org.winterblade.minecraft.mods.needs.mixins.ChatLevelMixin;
import org.winterblade.minecraft.mods.needs.mixins.ChatMixin;
import org.winterblade.minecraft.mods.needs.mixins.ScoreboardMixin;
import org.winterblade.minecraft.mods.needs.mixins.UiMixin;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;
import org.winterblade.minecraft.mods.needs.needs.INeedCapability;
import org.winterblade.minecraft.mods.needs.needs.NeedCapability;
import org.winterblade.minecraft.mods.needs.needs.attributes.AttackDamageNeed;
import org.winterblade.minecraft.mods.needs.needs.attributes.LuckNeed;
import org.winterblade.minecraft.mods.needs.needs.attributes.MaxHealthNeed;
import org.winterblade.minecraft.mods.needs.needs.vanilla.*;
import org.winterblade.minecraft.mods.needs.network.NetworkManager;

public class CoreRegistration {
    public static void register() {
        CapabilityManager.INSTANCE.register(INeedCapability.class, NeedCapability.Storage.INSTANCE, NeedCapability::new);

        registerNeeds();
        registerManipulators();
        registerMixins();
        registerActions();
        registerDebug();

        NetworkManager.register();
    }

    private static void registerNeeds() {
        // Need classes
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "custom", CustomNeed.class);
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "tick", CustomNeed.class, "onTick");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "food", FoodNeed.class, "hunger");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "health", HealthNeed.class, "hp");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "saturation", SaturationNeed.class);
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "yLevel", YLevelNeed.class, "y", "depth");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "light", LightLevelNeed.class, "lightlevel");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "breath", BreathNeed.class, "air");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "temperature", TemperatureNeed.class);
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "sunlight", SunlightNeed.class);
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "moonPhase", MoonPhaseNeed.class, "moon");

        // Attributes
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "maxHealth", MaxHealthNeed.class, "maxhp");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "attackDamage", AttackDamageNeed.class, "attack");
        NeedRegistry.INSTANCE.register(NeedsMod.MODID, "luck", LuckNeed.class);
    }

    private static void registerMixins() {
        // Mixins
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "scoreboard", ScoreboardMixin.class, "score");
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "chatOnChanged", ChatMixin.class);
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "chatOnLevel", ChatLevelMixin.class, "chat");
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "ui", UiMixin.class, "gui");
    }

    private static void registerManipulators() {
        // Manipulators
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "itemUsed", ItemUsedManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "perHour", PerHourManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "onDeath", OnDeathManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "onNeedChanged", OnNeedChangedManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "onBlock", NearBlockManipulator.class, "block", "nearBlock", "aroundBlock");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "holding", HoldingManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "attacking", AttackingManipulator.class, "onAttack", "attack");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "hurt", HurtManipulator.class, "damaged", "attacked");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "heal", HealManipulator.class, "healed");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "biome", BiomeManipulator.class, "inBiome", "inBiomeType", "biomeType");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "lookingAt", LookingAtManipulator.class, "lookingAtBlock");
    }

    private static void registerActions() {
        // Actions
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "potionEffect", PotionEffectLevelAction.class);
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "adjustNeed", AdjustNeedLevelAction.class);
    }

    private static void registerDebug() {
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "tickDebug", TestingTickLevelAction.class);
    }
}

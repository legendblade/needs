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
        NeedRegistry.INSTANCE.register("custom", CustomNeed.class);
        NeedRegistry.INSTANCE.register("food", FoodNeed.class, "hunger");
        NeedRegistry.INSTANCE.register("health", HealthNeed.class, "hp");
        NeedRegistry.INSTANCE.register("saturation", SaturationNeed.class);
        NeedRegistry.INSTANCE.register("ylevel", YLevelNeed.class, "y", "depth");
        NeedRegistry.INSTANCE.register("light", LightLevelNeed.class, "lightlevel");
        NeedRegistry.INSTANCE.register("breath", BreathNeed.class, "air");
        NeedRegistry.INSTANCE.register("temperature", TemperatureNeed.class);
        NeedRegistry.INSTANCE.register("sunlight", SunlightNeed.class);
        NeedRegistry.INSTANCE.register("moonPhase", MoonPhaseNeed.class, "moon");

        // Attributes
        NeedRegistry.INSTANCE.register("maxhealth", MaxHealthNeed.class, "maxhp");
        NeedRegistry.INSTANCE.register("attackDamage", AttackDamageNeed.class, "attack");
        NeedRegistry.INSTANCE.register("luck", LuckNeed.class);
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
        ManipulatorRegistry.INSTANCE.register("onBlock", NearBlockManipulator.class, "block", "nearBlock", "aroundBlock");
        ManipulatorRegistry.INSTANCE.register("holding", HoldingManipulator.class);
        ManipulatorRegistry.INSTANCE.register("attacking", AttackingManipulator.class, "onAttack", "attack");
        ManipulatorRegistry.INSTANCE.register("hurt", HurtManipulator.class, "damaged", "attacked");
        ManipulatorRegistry.INSTANCE.register("heal", HealManipulator.class, "healed");
        ManipulatorRegistry.INSTANCE.register("biome", BiomeManipulator.class, "inBiome", "inBiomeType", "biomeType");
        ManipulatorRegistry.INSTANCE.register("lookingAt", LookingAtManipulator.class, "lookingAtBlock");
    }

    private static void registerActions() {
        // Actions
        LevelActionRegistry.INSTANCE.register("potionEffect", PotionEffectLevelAction.class);
        LevelActionRegistry.INSTANCE.register("adjustNeed", AdjustNeedLevelAction.class);
    }

    private static void registerDebug() {
        LevelActionRegistry.INSTANCE.register("tickDebug", TestingTickLevelAction.class);
    }
}

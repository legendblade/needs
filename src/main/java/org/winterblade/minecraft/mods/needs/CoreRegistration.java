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
        CapabilityManager.INSTANCE.register(
                ItemUsedCountManipulator.IItemUsedCountCapability.class,
                ItemUsedCountManipulator.ItemUsedCountCapability.Storage.INSTANCE,
                ItemUsedCountManipulator.ItemUsedCountCapability::new
        );

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
        NeedRegistry.INSTANCE.register("minecraft", "food", FoodNeed.class, "hunger");
        NeedRegistry.INSTANCE.register("minecraft", "health", HealthNeed.class, "hp");
        NeedRegistry.INSTANCE.register("minecraft", "saturation", SaturationNeed.class);
        NeedRegistry.INSTANCE.register("minecraft", "yLevel", YLevelNeed.class, "y", "depth");
        NeedRegistry.INSTANCE.register("minecraft", "light", LightLevelNeed.class, "lightlevel");
        NeedRegistry.INSTANCE.register("minecraft", "breath", BreathNeed.class, "air");
        NeedRegistry.INSTANCE.register("minecraft", "temperature", TemperatureNeed.class);
        NeedRegistry.INSTANCE.register("minecraft", "sunlight", SunlightNeed.class);
        NeedRegistry.INSTANCE.register("minecraft", "moonPhase", MoonPhaseNeed.class, "moon");
        NeedRegistry.INSTANCE.register("minecraft", "scoreboard", ScoreboardNeed.class, "score");

        // Attributes
        NeedRegistry.INSTANCE.register("minecraft", "maxHealth", MaxHealthNeed.class, "maxhp");
        NeedRegistry.INSTANCE.register("minecraft", "attackDamage", AttackDamageNeed.class, "attack");
        NeedRegistry.INSTANCE.register("minecraft", "luck", LuckNeed.class);
    }

    private static void registerMixins() {
        // Mixins
        MixinRegistry.INSTANCE.register("minecraft", "scoreboard", ScoreboardMixin.class, "score");
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "chatOnChanged", ChatMixin.class);
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "chatOnLevel", ChatLevelMixin.class, "chat");
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "ui", UiMixin.class, "gui");
    }

    private static void registerManipulators() {
        // Base game type things
        ManipulatorRegistry.INSTANCE.register("minecraft", "itemUsed", ItemUsedManipulator.class);
        ManipulatorRegistry.INSTANCE.register("minecraft", "countedItemUse", ItemUsedCountManipulator.class, "itemUsedOnce");
        ManipulatorRegistry.INSTANCE.register("minecraft", "perHour", PerHourManipulator.class);
        ManipulatorRegistry.INSTANCE.register("minecraft", "onDeath", OnDeathManipulator.class);
        ManipulatorRegistry.INSTANCE.register("minecraft", "onBlock", NearBlockManipulator.class, "block", "nearBlock", "aroundBlock");
        ManipulatorRegistry.INSTANCE.register("minecraft", "holding", HoldingManipulator.class);
        ManipulatorRegistry.INSTANCE.register("minecraft", "attacking", AttackingManipulator.class, "onAttack", "attack");
        ManipulatorRegistry.INSTANCE.register("minecraft", "hurt", HurtManipulator.class, "damaged", "attacked");
        ManipulatorRegistry.INSTANCE.register("minecraft", "heal", HealManipulator.class, "healed");
        ManipulatorRegistry.INSTANCE.register("minecraft", "biome", BiomeManipulator.class, "inBiome", "inBiomeType", "biomeType");
        ManipulatorRegistry.INSTANCE.register("minecraft", "lookingAt", LookingAtManipulator.class, "lookingAtBlock");
        ManipulatorRegistry.INSTANCE.register("minecraft", "slept", SleepingManipulator.class, "woken");

        // Need specifics
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "onNeedChanged", OnNeedChangedManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "linkChain", LinkedNeedManipulator.class, "link", "chain");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "tick", TickManipulator.class, "onTick");
    }

    private static void registerActions() {
        // Actions
        LevelActionRegistry.INSTANCE.register("minecraft", "potionEffect", PotionEffectLevelAction.class);
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "adjustNeed", AdjustNeedLevelAction.class);
    }

    private static void registerDebug() {
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "tickDebug", TestingTickLevelAction.class);
    }
}

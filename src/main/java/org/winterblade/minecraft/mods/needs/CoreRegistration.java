package org.winterblade.minecraft.mods.needs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import org.winterblade.minecraft.mods.needs.actions.*;
import org.winterblade.minecraft.mods.needs.api.registries.*;
import org.winterblade.minecraft.mods.needs.capabilities.CapabilityCloner;
import org.winterblade.minecraft.mods.needs.capabilities.latch.ILatchedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.latch.LatchedCapability;
import org.winterblade.minecraft.mods.needs.manipulators.conditionals.AndConditionalManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.conditionals.NotConditionalManipulator;
import org.winterblade.minecraft.mods.needs.manipulators.conditionals.OrConditionalManipulator;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.IItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.ItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.manipulators.*;
import org.winterblade.minecraft.mods.needs.manipulators.conditionals.XorConditionalManipulator;
import org.winterblade.minecraft.mods.needs.mixins.*;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;
import org.winterblade.minecraft.mods.needs.capabilities.customneed.INeedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.customneed.NeedCapability;
import org.winterblade.minecraft.mods.needs.needs.attributes.AttackDamageNeed;
import org.winterblade.minecraft.mods.needs.needs.attributes.LuckNeed;
import org.winterblade.minecraft.mods.needs.needs.attributes.MaxHealthNeed;
import org.winterblade.minecraft.mods.needs.needs.vanilla.*;
import org.winterblade.minecraft.mods.needs.network.NetworkManager;

public class CoreRegistration {
    public static void register() {
        registerCapabilities();

        registerNeeds();
        registerManipulators();
        registerConditions();
        registerTriggers();
        registerMixins();
        registerActions();
        registerDebug();

        NetworkManager.register();
    }

    private static void registerCapabilities() {
        MinecraftForge.EVENT_BUS.register(CapabilityCloner.class);

        CapabilityManager.INSTANCE.register(INeedCapability.class, NeedCapability.Storage.INSTANCE, NeedCapability::new);
        CapabilityManager.INSTANCE.register(
                IItemUsedCountCapability.class,
                ItemUsedCountCapability.Storage.INSTANCE,
                ItemUsedCountCapability::new
        );
        CapabilityManager.INSTANCE.register(
                ILatchedCapability.class,
                LatchedCapability.Storage.INSTANCE,
                LatchedCapability::new
        );
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
        NeedRegistry.INSTANCE.register("minecraft", "distance", DistanceFromSpawnNeed.class);

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
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "iconBar", IconBarMixin.class);
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "progressBar", ProgressBarMixin.class, "bar");
        MixinRegistry.INSTANCE.register(NeedsMod.MODID, "hideBar", HideBarMixin.class, "hideHud");
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
        ManipulatorRegistry.INSTANCE.register("minecraft", "wearing", WearingManipulator.class);

        // Need specifics
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "onNeedChanged", OnNeedChangedManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "linkChain", LinkedNeedManipulator.class, "link", "chain");
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "tick", TickManipulator.class, "onTick");

        // Conditionals
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "or", OrConditionalManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "not", NotConditionalManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "and", AndConditionalManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "xor", XorConditionalManipulator.class);
        ManipulatorRegistry.INSTANCE.register(NeedsMod.MODID, "latch", LatchedManipulator.class, "latched");
    }

    private static void registerConditions() {
        // Base game type things
        ConditionRegistry.INSTANCE.register("minecraft", "onBlock", NearBlockManipulator.class, "block", "nearBlock", "aroundBlock");
        ConditionRegistry.INSTANCE.register("minecraft", "holding", HoldingManipulator.class);
        ConditionRegistry.INSTANCE.register("minecraft", "biome", BiomeManipulator.class, "inBiome", "inBiomeType", "biomeType");
        ConditionRegistry.INSTANCE.register("minecraft", "lookingAt", LookingAtManipulator.class, "lookingAtBlock");
        ConditionRegistry.INSTANCE.register("minecraft", "wearing", WearingManipulator.class);

        // Conditionals
        ConditionRegistry.INSTANCE.register(NeedsMod.MODID, "or", OrConditionalManipulator.class);
        ConditionRegistry.INSTANCE.register(NeedsMod.MODID, "not", NotConditionalManipulator.class);
        ConditionRegistry.INSTANCE.register(NeedsMod.MODID, "and", AndConditionalManipulator.class);
        ConditionRegistry.INSTANCE.register(NeedsMod.MODID, "xor", XorConditionalManipulator.class);
        ConditionRegistry.INSTANCE.register(NeedsMod.MODID, "latch", LatchedManipulator.class, "latched");
    }

    private static void registerTriggers() {
        // Base game type things
        TriggerRegistry.INSTANCE.register("minecraft", "itemUsed", ItemUsedManipulator.class);
        TriggerRegistry.INSTANCE.register("minecraft", "countedItemUse", ItemUsedCountManipulator.class, "itemUsedOnce");
        TriggerRegistry.INSTANCE.register("minecraft", "perHour", PerHourManipulator.class);
        TriggerRegistry.INSTANCE.register("minecraft", "onDeath", OnDeathManipulator.class);
        TriggerRegistry.INSTANCE.register("minecraft", "onBlock", NearBlockManipulator.class, "block", "nearBlock", "aroundBlock");
        TriggerRegistry.INSTANCE.register("minecraft", "holding", HoldingManipulator.class);
        TriggerRegistry.INSTANCE.register("minecraft", "attacking", AttackingManipulator.class, "onAttack", "attack");
        TriggerRegistry.INSTANCE.register("minecraft", "hurt", HurtManipulator.class, "damaged", "attacked");
        TriggerRegistry.INSTANCE.register("minecraft", "heal", HealManipulator.class, "healed");
        TriggerRegistry.INSTANCE.register("minecraft", "biome", BiomeManipulator.class, "inBiome", "inBiomeType", "biomeType");
        TriggerRegistry.INSTANCE.register("minecraft", "lookingAt", LookingAtManipulator.class, "lookingAtBlock");
        TriggerRegistry.INSTANCE.register("minecraft", "slept", SleepingManipulator.class, "woken");
        TriggerRegistry.INSTANCE.register("minecraft", "wearing", WearingManipulator.class);

        // Need specifics
        TriggerRegistry.INSTANCE.register(NeedsMod.MODID, "onNeedChanged", OnNeedChangedManipulator.class);
        TriggerRegistry.INSTANCE.register(NeedsMod.MODID, "tick", TickManipulator.class, "onTick");

        // Conditionals
        TriggerRegistry.INSTANCE.register(NeedsMod.MODID, "latch", LatchedManipulator.class, "latched");
    }

    private static void registerActions() {
        // Actions
        LevelActionRegistry.INSTANCE.register("minecraft", "potionEffect", PotionEffectLevelAction.class);
        LevelActionRegistry.INSTANCE.register("minecraft", "command", CommandLevelAction.class);
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "adjustNeed", AdjustNeedLevelAction.class);
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "overlay", OverlayAction.class, "vignette");
    }

    private static void registerDebug() {
        LevelActionRegistry.INSTANCE.register(NeedsMod.MODID, "tickDebug", TestingTickLevelAction.class);
    }
}

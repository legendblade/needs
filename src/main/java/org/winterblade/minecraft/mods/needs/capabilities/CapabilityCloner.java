package org.winterblade.minecraft.mods.needs.capabilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.capabilities.customneed.INeedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.customneed.NeedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.IItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.ItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.capabilities.latch.ILatchedCapability;
import org.winterblade.minecraft.mods.needs.capabilities.latch.LatchedCapability;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedCountManipulator;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

public class CapabilityCloner {
    @SubscribeEvent
    public static void attach(final AttachCapabilitiesEvent<Entity> evt) {
        if (!(evt.getObject() instanceof PlayerEntity)) return;

        evt.addCapability(new ResourceLocation("needs:custom_needs"), new NeedCapability.Provider());
        evt.addCapability(new ResourceLocation("needs:latched"), new LatchedCapability.Provider());
        evt.addCapability(new ResourceLocation("needs:item_used_count"), new ItemUsedCountCapability.Provider());
    }

    @SubscribeEvent
    protected static void onClone(final PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        // Oof
        final PlayerEntity original = event.getOriginal();

        // Handle custom need
        final LazyOptional<INeedCapability> customNeedCap = original
                .getCapability(CustomNeed.CAPABILITY);

        customNeedCap
            .map(INeedCapability::getValues)
            .ifPresent((values) ->
                    event
                        .getPlayer()
                            .getCapability(CustomNeed.CAPABILITY)
                            .ifPresent((cap) -> values.forEach(cap::setValue))
            );
        customNeedCap
            .map(INeedCapability::getLevelAdjustments)
            .ifPresent((values) ->
                    event
                        .getPlayer()
                        .getCapability(CustomNeed.CAPABILITY)
                        .ifPresent((cap) -> values.forEach((need, v) -> v.forEach((level, value) -> {
                            cap.storeLevelAdjustment(need, level, value);
                        })))
            );

        // Latched caps
        original
            .getCapability(LatchedCapability.CAPABILITY)
            .map(ILatchedCapability::getValues)
            .ifPresent((values) ->
                    event
                        .getPlayer()
                            .getCapability(LatchedCapability.CAPABILITY)
                            .ifPresent((cap) -> values.forEach(cap::setValue)));


        // Item use
        original
            .getCapability(ItemUsedCountManipulator.CAPABILITY)
            .map(IItemUsedCountCapability::getStorage)
            .ifPresent((values) ->
                    event
                        .getPlayer()
                            .getCapability(ItemUsedCountManipulator.CAPABILITY)
                            .ifPresent((cap) -> values.forEach((storageKey, v) -> v.forEach((item, value) -> {
                                cap.getStorage(storageKey).put(item, value);
                            })))
            );
    }
}

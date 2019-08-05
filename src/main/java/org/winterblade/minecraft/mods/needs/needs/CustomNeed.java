package org.winterblade.minecraft.mods.needs.needs;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@SuppressWarnings("WeakerAccess")
public class CustomNeed extends Need {
    static {
        CAPABILITY = null;
    }

    @CapabilityInject(INeedCapability.class)
    public static final Capability<INeedCapability> CAPABILITY;

    @Expose
    protected String name;
    @Expose
    protected int min;
    @Expose
    protected int max;
    @Expose
    protected int initial;
    @Expose
    protected boolean resetOnDeath;

    public CustomNeed() {
        min = 0;
        max = 100;
        initial = 50;
    }

    @Override
    public boolean allowMultiple() {
        return true;
    }

    @Override
    public void onCreated() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public String getName() {
        return name;
    }

    public double getMin(final PlayerEntity player) {
        return min;
    }

    public double getMax(final PlayerEntity player) {
        return max;
    }

    public double getInitial() {
        return initial;
    }

    public boolean isResetOnDeath() {
        return resetOnDeath;
    }

    public void setName(final String baseName) {
        name = baseName;
    }

    @Override
    public void initialize(final PlayerEntity player) {
        setValue(player, getInitial(), getInitial());
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player
            .getCapability(CAPABILITY)
                .map((cap) -> cap.getValue(getName()))
                .orElse(getInitial());
    }

    @Override
    public double setValue(final PlayerEntity player, final double newValue, final double adjustAmount) {
        player
            .getCapability(CAPABILITY)
            .ifPresent((cap) -> cap.setValue(getName(), newValue));
        return newValue;
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return player
            .getCapability(CAPABILITY)
                .map((cap) -> cap.isInitialized(getName()))
                .orElse(false);
    }

    @SubscribeEvent
    protected void onDeath(final LivingDeathEvent event) {
        if (event.isCanceled() || !isResetOnDeath() || !(event.getEntity() instanceof PlayerEntity)) return;

        final PlayerEntity entity = (PlayerEntity) event.getEntity();
        setValue(entity, getInitial(), getValue(entity) - getInitial());
    }

    @SubscribeEvent
    protected void onClone(final PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        // Oof
        event
            .getOriginal()
                .getCapability(CAPABILITY)
                .map(INeedCapability::getValues)
                .ifPresent((values) ->
                        event
                            .getEntityPlayer()
                                .getCapability(CAPABILITY)
                                .ifPresent((cap) -> values.forEach(cap::setValue))
                );
    }
}
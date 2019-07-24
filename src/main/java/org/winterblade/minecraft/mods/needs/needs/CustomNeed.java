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
import org.winterblade.minecraft.mods.needs.api.Need;

@SuppressWarnings("WeakerAccess")
public class CustomNeed extends Need {
    static {
        CAPABILITY = null;
        CapabilityManager.INSTANCE.register(ICustomNeedCapability.class, CustomNeedCapability.Storage.INSTANCE, CustomNeedCapability::new);
    }

    @CapabilityInject(ICustomNeedCapability.class)
    public static final Capability<ICustomNeedCapability> CAPABILITY;

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
    public void onCreated() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public String getName() {
        return name;
    }

    public int getMin(PlayerEntity player) {
        return min;
    }

    public int getMax(PlayerEntity player) {
        return max;
    }

    public int getInitial() {
        return initial;
    }

    public boolean isResetOnDeath() {
        return resetOnDeath;
    }

    public void setName(String baseName) {
        name = baseName;
    }

    @Override
    public void initialize(PlayerEntity player) {
        setValue(player, getInitial());
    }

    @Override
    public int getValue(PlayerEntity player) {
        return player
            .getCapability(CAPABILITY)
                .map((cap) -> cap.getValue(getName()))
                .orElse(getInitial());
    }

    @Override
    public void setValue(PlayerEntity player, int newValue) {
        player
            .getCapability(CAPABILITY)
            .ifPresent((cap) -> cap.setValue(getName(), newValue));
    }

    @Override
    public boolean isValueInitialized(PlayerEntity player) {
        return player
            .getCapability(CAPABILITY)
                .map((cap) -> cap.isInitialized(getName()))
                .orElse(false);
    }

    @SubscribeEvent
    protected void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isResetOnDeath() || !(event.getEntity() instanceof PlayerEntity)) return;

        setValue((PlayerEntity) event.getEntity(), getInitial());
    }

    @SubscribeEvent
    protected void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        // Oof
        event
            .getOriginal()
                .getCapability(CAPABILITY)
                .map(ICustomNeedCapability::getValues)
                .ifPresent((values) ->
                        event
                            .getEntityPlayer()
                                .getCapability(CAPABILITY)
                                .ifPresent((cap) -> values.forEach(cap::setValue))
                );
    }
}
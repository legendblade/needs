package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

public class OnDeathManipulator extends BaseManipulator {
    @Expose
    private int amount;

    public OnDeathManipulator() {
        this.amount = -10;
    }

    @SubscribeEvent
    protected void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled()) parent.adjustValue(event.getEntity(), amount, this);
    }
}

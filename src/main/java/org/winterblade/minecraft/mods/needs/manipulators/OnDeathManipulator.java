package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.NeedExpressionContext;

public class OnDeathManipulator extends BaseManipulator {
    @Expose
    private NeedExpressionContext amount;

    @SubscribeEvent
    protected void onDeath(final LivingDeathEvent event) {
        if (event.getEntity().world.isRemote) return;

        amount.setIfRequired(NeedExpressionContext.CURRENT_NEED_VALUE, () -> parent.getValue((PlayerEntity) event.getEntity()));
        if (!event.isCanceled()) parent.adjustValue(event.getEntity(), amount.get(), this);
    }
}

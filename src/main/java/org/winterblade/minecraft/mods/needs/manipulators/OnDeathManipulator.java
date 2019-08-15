package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@Document(description = "Triggered when the player dies.")
public class OnDeathManipulator extends DimensionBasedManipulator {
    @Expose
    @Document(description = "The amount to change by.")
    private NeedExpressionContext amount;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        super.validate(need);
    }

    @SubscribeEvent
    protected void onDeath(final LivingDeathEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof PlayerEntity) || event.isCanceled()) return;

        final PlayerEntity pl = (PlayerEntity) event.getEntity();
        if (failsDimensionCheck(pl)) return;
        amount.setCurrentNeedValue(parent, pl);
        parent.adjustValue(pl, amount.apply(pl), this);
    }
}

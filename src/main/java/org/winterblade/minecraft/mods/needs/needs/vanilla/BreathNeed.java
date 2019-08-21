package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;

@Document(description = "Tracks the player's breath meter")
public class BreathNeed extends ConcealableHudNeed {

    public BreathNeed() {
        super(RenderGameOverlayEvent.ElementType.AIR);
    }

    @Override
    public String getName() {
        return "Breath";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return player.getMaxAir();
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.getAir();
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }

    @Override
    protected double setValue(final PlayerEntity player, final double newValue, final double adjustAmount) {
        player.setAir((int) newValue);
        return newValue;
    }

}

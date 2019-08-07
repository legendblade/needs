package org.winterblade.minecraft.mods.needs.needs.vanilla;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.ReadOnlyNeed;

@Document(description = "Tracks the current Y-level of the player")
public class YLevelNeed extends ReadOnlyNeed {
    @Override
    public String getName() {
        return "Y-Level";
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return 0;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return player.world.getMaxHeight();
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return player.getHeight();
    }
}

package org.winterblade.minecraft.mods.needs.api.actions;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;

@JsonAdapter(LevelActionRegistry.class)
public abstract class LevelAction {
    public abstract String getName();

    protected void onEntered(PlayerEntity player) {

    }

    protected void onExited(PlayerEntity player) {

    }

    protected void onContinuousStart(PlayerEntity player) {

    }

    protected void onContinuousTick(PlayerEntity player) {

    }

    protected void onContinuousEnd(PlayerEntity player) {

    }
}

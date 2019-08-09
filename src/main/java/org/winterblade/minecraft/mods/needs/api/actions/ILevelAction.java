package org.winterblade.minecraft.mods.needs.api.actions;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.registries.LevelActionRegistry;

@JsonAdapter(LevelActionRegistry.class)
public interface ILevelAction {
    String getName();

    void onLoaded(Need parentNeed, NeedLevel parentLevel);

    void onEntered(Need need, NeedLevel level, PlayerEntity player);

    void onExited(Need need, NeedLevel level, PlayerEntity player);

    void onContinuousStart(Need need, NeedLevel level, PlayerEntity player);

    void onContinuousEnd(Need need, NeedLevel level, PlayerEntity player);
}

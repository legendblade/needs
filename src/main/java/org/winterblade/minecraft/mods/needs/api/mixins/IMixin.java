package org.winterblade.minecraft.mods.needs.api.mixins;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;

@JsonAdapter(MixinRegistry.class)
public interface IMixin {
    void onCreated (Need need);
}

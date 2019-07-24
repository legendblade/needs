package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.Need;

@JsonAdapter(ManipulatorRegistry.class)
public interface IManipulator {
    void OnCreated(Need need);

    String FormatMessage(String needName, int amount, int newValue);
}

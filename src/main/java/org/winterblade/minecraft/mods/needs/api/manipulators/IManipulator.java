package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.Need;

@JsonAdapter(ManipulatorRegistry.class)
public interface IManipulator {
    void onCreated(Need need);

    String formatMessage(String needName, double amount, double newValue);
}

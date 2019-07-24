package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.Need;

@JsonAdapter(BaseManipulator.Deserializer.class)
public interface IManipulator {
    void OnCreated(Need need);

    boolean isSilent();

    String FormatMessage(String needName, int amount, int newValue);
}

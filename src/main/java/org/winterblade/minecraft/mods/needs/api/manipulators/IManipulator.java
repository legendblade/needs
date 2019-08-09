package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@JsonAdapter(ManipulatorRegistry.class)
public interface IManipulator {
    /**
     * Called after deserialization to validate the need has everything it needs
     * to function.
     * @param need The need the manipulator is in
     * @throws IllegalArgumentException If a parameter is invalid
     */
    void validate(Need need) throws IllegalArgumentException;

    /**
     * Called in order to finish loading the manipulator
     * @param need The need the manipulator is in
     */
    void onLoaded(Need need);

    double getLowestToSetNeed();

    double getHighestToSetNeed();
}

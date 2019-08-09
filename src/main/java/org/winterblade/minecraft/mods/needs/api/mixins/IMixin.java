package org.winterblade.minecraft.mods.needs.api.mixins;

import com.google.gson.annotations.JsonAdapter;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.MixinRegistry;

@JsonAdapter(MixinRegistry.class)
public interface IMixin {
    /**
     * Called after deserialization to validate the mixin has everything it needs
     * to function.
     * @param need The need the mixin is in
     * @throws IllegalArgumentException If a parameter is invalid
     */
    void validate(Need need) throws IllegalArgumentException;

    /**
     * Called when the mixin is being loaded
     * @param need The need the manipulator is in
     */
    void onLoaded(Need need);
}

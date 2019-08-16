package org.winterblade.minecraft.mods.needs.api;

import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.ConditionRegistry;

import java.util.function.Predicate;

@JsonAdapter(ConditionRegistry.class)
@Document(description = "A condition to run in order to verify if the conditional manipulator should complete.")
public interface ICondition extends Predicate<PlayerEntity> {
    void validateCondition(Need parentNeed, ConditionalManipulator parentCondition) throws IllegalArgumentException;

    default void onConditionLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {}

    default void onConditionUnloaded() {}

    double getAmount(PlayerEntity player);
}

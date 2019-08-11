package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

import java.util.Set;

public abstract class DimensionBasedManipulator extends BaseManipulator {
    @Expose
    @Document(description = "Optional list of dimension IDs to limit the effect to.")
    @OptionalField(defaultValue = "All Dimensions")
    protected Set<Integer> dimensions;

    protected boolean checkDims;

    @Override
    public void onLoaded() {
        super.onLoaded();
        checkDims = !dimensions.isEmpty();
    }

    protected boolean failsDimensionCheck(final PlayerEntity player) {
        return checkDims && !dimensions.contains(player.world.getDimension().getType().getId());
    }
}

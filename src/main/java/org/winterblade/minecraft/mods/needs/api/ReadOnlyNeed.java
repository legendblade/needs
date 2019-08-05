package org.winterblade.minecraft.mods.needs.api;

import com.google.gson.JsonParseException;
import net.minecraft.entity.player.PlayerEntity;

public abstract class ReadOnlyNeed extends CachedTickingNeed {
    @Override
    public void onCreated() {
        if (0 < getManipulators().size()) throw new JsonParseException("Read-only need " + getName() + " cannot have manipulators.");
        super.onCreated();
    }

    @Override
    protected double setValue(final PlayerEntity player, final double newValue, final double adjustAmount) {
        throw new IllegalStateException("This need cannot be updated.");
    }

    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return true;
    }
}

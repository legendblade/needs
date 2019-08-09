package org.winterblade.minecraft.mods.needs.api.needs;

import com.google.gson.JsonParseException;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;

@Document(description = "A read-only need can be read by other needs, and have levels assigned to it, but cannot be changed through manipulators or level actions")
public abstract class ReadOnlyNeed extends CachedTickingNeed {
    @Override
    public void onLoaded() {
        if (0 < getManipulators().size()) throw new JsonParseException("Read-only need " + getName() + " cannot have manipulators.");
        super.onLoaded();
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

package org.winterblade.minecraft.mods.needs.needs;

import javax.annotation.Nonnull;
import java.util.Map;

interface ICustomNeedCapability {
    int getValue(String id);

    void setValue(String id, int value);

    boolean isInitialized(String id);

    @Nonnull
    Map<String, Integer> getValues();
}

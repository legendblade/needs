package org.winterblade.minecraft.mods.needs.capabilities.latch;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ILatchedCapability {
    boolean lastValue(String key);

    void setValue(String key, boolean value);

    @Nonnull
    Map<String, Boolean> getValues();
}

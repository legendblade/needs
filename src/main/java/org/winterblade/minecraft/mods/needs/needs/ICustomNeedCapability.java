package org.winterblade.minecraft.mods.needs.needs;

import javax.annotation.Nonnull;
import java.util.Map;

interface ICustomNeedCapability {
    double getValue(String id);

    void setValue(String id, double value);

    boolean isInitialized(String id);

    @Nonnull
    Map<String, Double> getValues();
}

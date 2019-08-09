package org.winterblade.minecraft.mods.needs.needs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface INeedCapability {
    double getValue(String id);

    void setValue(String id, double value);

    boolean isInitialized(String id);

    @Nonnull
    Map<String, Double> getValues();

    void storeLevelAdjustment(String needName, String levelName, double adjustment);

    double getLevelAdjustment(String needName, String levelName);

    Map<String,Map<String,Double>> getLevelAdjustments();

}

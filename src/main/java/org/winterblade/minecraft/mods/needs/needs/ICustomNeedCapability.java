package org.winterblade.minecraft.mods.needs.needs;

import java.util.Map;

interface ICustomNeedCapability {
    int getValue(String id);

    void setValue(String id, int value);

    boolean isInitialized(String id);

    Map<String, Integer> getValues();
}

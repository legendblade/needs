package org.winterblade.minecraft.mods.needs.capabilities.itemuse;

import java.util.Map;

public interface IItemUsedCountCapability {
    Map<String, Map<String, ItemUseStorage>> getStorage();

    Map<String, ItemUseStorage> getStorage(String key);
}

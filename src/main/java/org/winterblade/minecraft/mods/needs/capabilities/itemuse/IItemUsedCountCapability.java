package org.winterblade.minecraft.mods.needs.capabilities.itemuse;

import javax.annotation.Nonnull;
import java.util.Map;

public interface IItemUsedCountCapability {
    @Nonnull
    Map<String, Map<String, ItemUseStorage>> getStorage();

    Map<String, ItemUseStorage> getStorage(String key);
}

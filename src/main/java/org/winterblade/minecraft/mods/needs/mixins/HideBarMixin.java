package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraftforge.common.MinecraftForge;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.LocalCacheUpdatedEvent;
import org.winterblade.minecraft.mods.needs.api.mixins.BaseMixin;
import org.winterblade.minecraft.mods.needs.api.needs.IHasHidableHudElement;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;

import java.util.ArrayList;
import java.util.List;

@Document(description = "Usable on certain needs (by default `health`, `breath`, and `food`) in order to hide the " +
        "associated UI bar. Other mods may add in additional bars that can be hidden.")
public class HideBarMixin extends BaseMixin {
    private static List<IHasHidableHudElement> localCache;

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(HideBarMixin::onSync);
    }

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        super.validate(need);
        if (!(need instanceof IHasHidableHudElement)) throw new IllegalArgumentException("Unable to hide the HUD for " + need.getName());
    }

    @Override
    public void onLoaded(final Need need) {
        super.onLoaded(need);
        need.enableSyncing();
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    protected static void onSync(final LocalCacheUpdatedEvent event) {
        // Unload any we had
        if (localCache != null) localCache.forEach(IHasHidableHudElement::unloadConcealer);
        localCache = new ArrayList<>();

        // Now find any new ones:
        NeedRegistry.INSTANCE.getLocalCache().forEach((key, value) -> {
            final Need need = value.getNeed().get();
            if (!(need instanceof IHasHidableHudElement) || need.getMixins().stream().noneMatch((m) -> m instanceof HideBarMixin)) return;

            final IHasHidableHudElement concealed = (IHasHidableHudElement) need;
            localCache.add(concealed);
            concealed.loadConcealer();
        });
    }
}

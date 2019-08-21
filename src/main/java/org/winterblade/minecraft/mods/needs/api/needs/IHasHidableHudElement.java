package org.winterblade.minecraft.mods.needs.api.needs;

import org.winterblade.minecraft.mods.needs.mixins.HideBarMixin;

public interface IHasHidableHudElement {
    /**
     * Called when the {@link HideBarMixin} is loading. This is called once per need list sync on the client
     */
    void loadConcealer();

    /**
     * Called when the {@link HideBarMixin} is unloading. This is called once per need list sync on the client
     */
    void unloadConcealer();
}

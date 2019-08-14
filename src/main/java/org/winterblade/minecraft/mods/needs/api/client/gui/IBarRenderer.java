package org.winterblade.minecraft.mods.needs.api.client.gui;

import org.winterblade.minecraft.mods.needs.api.needs.LocalCachedNeed;
import org.winterblade.minecraft.mods.needs.mixins.BarMixin;

public interface IBarRenderer {
    void render(LocalCachedNeed need, BarMixin mx, int x, int y);
}

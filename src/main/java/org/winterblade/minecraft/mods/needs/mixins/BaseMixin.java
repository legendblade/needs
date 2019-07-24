package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraftforge.fml.common.Mod;
import org.winterblade.minecraft.mods.needs.api.Need;

@Mod.EventBusSubscriber
public abstract class BaseMixin {
    protected final Need need;

    public BaseMixin(Need need) {
        this.need = need;
    }
}

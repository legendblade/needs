package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraftforge.fml.common.Mod;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.mixins.IMixin;

@Mod.EventBusSubscriber
public abstract class BaseMixin implements IMixin {
    protected Need need;

    @Override
    public void onLoaded(final Need need) {
        this.need = need;
    }
}

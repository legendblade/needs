package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraftforge.eventbus.api.Event;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

public class LocalNeedLevelEvent extends Event {
    private final Need need;
    private final NeedLevel level;

    public LocalNeedLevelEvent(final Need need, final NeedLevel level) {
        this.need = need;
        this.level = level;
    }

    public Need getNeed() {
        return need;
    }

    public NeedLevel getLevel() {
        return level;
    }
}

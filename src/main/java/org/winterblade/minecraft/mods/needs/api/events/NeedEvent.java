package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Event;
import org.winterblade.minecraft.mods.needs.api.Need;

public class NeedEvent extends Event {
    protected final Need need;
    protected final PlayerEntity player;

    public NeedEvent(final Need need, final PlayerEntity player) {
        this.need = need;
        this.player = player;
    }

    public Need getNeed() {
        return need;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}

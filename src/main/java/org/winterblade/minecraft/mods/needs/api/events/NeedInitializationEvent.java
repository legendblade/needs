package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import org.winterblade.minecraft.mods.needs.api.Need;

@SuppressWarnings({"unused", "WeakerAccess"})
public class NeedInitializationEvent extends NeedEvent {
    public NeedInitializationEvent(final Need need, final PlayerEntity player) {
        super(need, player);
    }

    /**
     * Called prior to a need being initialized on a given player; cancelling it will
     * prevent the need from being initialized on the player.
     *
     * Setting the amount during this event will do nothing.
     */
    @Cancelable
    public static class Pre extends NeedInitializationEvent {
        public Pre(final Need need, final PlayerEntity player) {
            super(need, player);
        }
    }

    /**
     * Called after a need has been initialized; you may adjust the amount during this event.
     */
    public static class Post extends NeedInitializationEvent {
        public Post(final Need need, final PlayerEntity player) {
            super(need, player);
        }
    }
}

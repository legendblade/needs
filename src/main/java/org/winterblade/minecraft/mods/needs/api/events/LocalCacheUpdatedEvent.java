package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the local need cache has been updated.
 *
 * Note: This event may be fired multiple times in succession, as such, listeners should implement a lazy-loading strategy
 */
public class LocalCacheUpdatedEvent extends Event {
    public LocalCacheUpdatedEvent() {
    }
}

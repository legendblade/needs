package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;

@SuppressWarnings({"unused", "WeakerAccess"})
public class NeedAdjustmentEvent extends NeedEvent {
    protected final IManipulator source;

    public NeedAdjustmentEvent(Need need, PlayerEntity player, IManipulator source) {
        super(need, player);
        this.source = source;
    }

    public IManipulator getSource() {
        return source;
    }

    /**
     * Called prior to a need being adjusted for the given player;
     * cancelling will prevent the player from being affected
     */
    @Cancelable
    public static class Pre extends NeedAdjustmentEvent {

        public Pre(Need need, PlayerEntity player, IManipulator source) {
            super(need, player, source);
        }
    }

    /**
     * Called after a need has been adjusted for the given player
     */
    public static class Post extends NeedAdjustmentEvent {
        private final double previous;
        private final double current;

        public Post(Need need, PlayerEntity player, IManipulator source, double previous, double current) {
            super(need, player, source);
            this.previous = previous;
            this.current = current;
        }

        /**
         * Gets the previous value for the need
         * @return  The previous value
         */
        public double getPrevious() {
            return previous;
        }

        /**
         * Gets the new value for the need
         * @return  The new value
         */
        public double getCurrent() {
            return current;
        }
    }
}

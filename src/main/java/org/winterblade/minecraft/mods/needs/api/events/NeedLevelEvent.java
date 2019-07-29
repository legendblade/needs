package org.winterblade.minecraft.mods.needs.api.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Event;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;

/**
 * Base event fired related to need levels
 */
public abstract class NeedLevelEvent extends Event {
    private final Need need;
    private final PlayerEntity player;
    private final NeedLevel level;

    public NeedLevelEvent(Need need, PlayerEntity player, NeedLevel level) {
        this.level = level;
        this.need = need;
        this.player = player;
    }

    public Need getNeed() {
        return need;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public NeedLevel getLevel() {
        return level;
    }

    /**
     * Fired after a need has changed levels
     */
    public static class Changed extends NeedLevelEvent {

        private final IManipulator source;
        private final double previousValue;
        private final double newValue;
        private final NeedLevel previousLevel;

        public Changed(Need need, PlayerEntity player, IManipulator source, double prevValue, double newValue, NeedLevel prevLevel, NeedLevel newLevel) {
            super(need, player, newLevel);
            this.source = source;
            this.previousValue = prevValue;
            this.newValue = newValue;
            this.previousLevel = prevLevel;
        }

        public IManipulator getSource() {
            return source;
        }

        public double getPreviousValue() {
            return previousValue;
        }

        public double getNewValue() {
            return newValue;
        }

        public NeedLevel getPreviousLevel() {
            return previousLevel;
        }
    }
}

package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedInitializationEvent;

@Document(description = "Use this mixin to mirror the need to a scoreboard entry of the same name as the need; the " +
        "value of the need will be rounded to the nearest whole number.")
public class ScoreboardMixin extends BaseMixin {
    @SubscribeEvent
    public void onInitialized(final NeedInitializationEvent.Post evt) {
        if (evt.getNeed() != need) return;
        getOrCreateScore(evt, evt.getNeed().getValue(evt.getPlayer()));
    }

    @SubscribeEvent
    public void onAdjusted(final NeedAdjustmentEvent.Post evt) {
        if (evt.getNeed() != need) return;

        final Score score = getOrCreateScore(evt, evt.getCurrent());
        if (score == null) return;

        score.setScorePoints((int) Math.round(evt.getCurrent()));
    }

    private static Score getOrCreateScore(final NeedEvent evt, final double initialValue) {
        final Scoreboard scoreboard = evt.getPlayer().getWorldScoreboard();
        //noinspection ConstantConditions
        if (scoreboard == null) return null;

        ScoreObjective objective = scoreboard.getOrCreateObjective(evt.getNeed().getName());

        // No, it really can be null...
        //noinspection ConstantConditions
        if (objective == null) {
            objective = scoreboard.addObjective(
                    evt.getNeed().getName(), // The internal name
                    ScoreCriteria.DUMMY, // The criteria type
                    new StringTextComponent(evt.getNeed().getName()), // The display name
                    ScoreCriteria.RenderType.INTEGER // How to display it
            );
        }

        Score score = scoreboard.getObjectivesForEntity(evt.getPlayer().getScoreboardName()).get(objective);

        // If the score didn't exist:
        if (score == null) {
            try {
                score = scoreboard.getOrCreateScore(evt.getPlayer().getScoreboardName(), objective);
                score.setScorePoints((int) Math.round(initialValue));
            } catch (final IllegalArgumentException e) {
                // gg Mojang; this will blow up due to overly long UNs, but the other method won't.
                NeedsMod.LOGGER.error("Unable to create scoreboard objective for '" + evt.getNeed().getName() + "'.", e);
            }
        }

        return score;
    }
}

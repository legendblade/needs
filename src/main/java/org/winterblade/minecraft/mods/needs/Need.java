package org.winterblade.minecraft.mods.needs;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.Expose;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.api.manipulators.IManipulator;

import java.util.List;

public class Need {
    @Expose
    public String name;

    @Expose
    public int min;

    @Expose
    public int max;

    @Expose
    public int initial;

    @Expose
    public boolean resetOnDeath;

    @Expose
    public boolean silent;

    @Expose
    private List<IManipulator> manipulators;

    public Need() {
        min = 0;
        max = 100;
        initial = 50;
    }

    public void OnCreated() {

    }

    public List<IManipulator> GetManipulators() {
        return ImmutableList.copyOf(manipulators);
    }

    public void AdjustScore(PlayerEntity p, int adjust, IManipulator source) {
        if (p == null || adjust == 0) return;
        Scoreboard scoreboard = p.world.getScoreboard();
        //noinspection ConstantConditions
        if (scoreboard == null) return;

        ScoreObjective objective = scoreboard.getOrCreateObjective(name);

        // No, it really can be null...
        //noinspection ConstantConditions
        if (objective == null) {
            // We're just going to assume, since there's no documentation...
            objective = scoreboard.addObjective(
                    name,
                    ScoreCriteria.DUMMY,
                    new StringTextComponent(name + " text"),
                    ScoreCriteria.RenderType.INTEGER
            );
        }

        Score score = scoreboard.getObjectivesForEntity(p.getScoreboardName()).get(objective);

        // If the score didn't exist:
        if (score == null) {
            try {
                score = scoreboard.getOrCreateScore(p.getScoreboardName(), objective);
                score.setScorePoints(initial);
            } catch (IllegalArgumentException e) {
                // gg Mojang; this will blow up due to overly long UNs, but the other method won't.
                NeedsMod.LOGGER.error("Unable to create scoreboard objective for '" + name + "'.", e);
            }
            return;
        }

        score.increaseScore(adjust);

        // Send a message about it:
        if (silent || source.isSilent()) return;
        p.sendStatusMessage(new StringTextComponent(source.FormatMessage(name, adjust, score.getScorePoints())), false);
    }

    public void AdjustScore(Entity entity, int adjust, IManipulator source) {
        if (!(entity instanceof PlayerEntity)) return;
        AdjustScore((PlayerEntity) entity, adjust, source);
    }
}

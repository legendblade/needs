package org.winterblade.minecraft.mods.needs.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.NeedsMod;

public abstract class ScoreHelper {
    /**
     * Gets or creates the score for the player
     * @param player        The player
     * @param name          The name of the score
     * @param initialValue  The initial value if created
     * @return The {@link Score}
     */
    public static Score getOrCreateScore(final PlayerEntity player, final String name, final double initialValue) {
        return getOrCreateScore(player, name, initialValue, ScoreCriteria.DUMMY);
    }

    /**
     * Gets or creates the score for the player
     * @param player        The player
     * @param name          The name of the score
     * @param initialValue  The initial value if created
     * @param criteria      The criterion to use for the score
     * @return The {@link Score}
     */
    public static Score getOrCreateScore(final PlayerEntity player, final String name, final double initialValue, final ScoreCriteria criteria) {
        final Scoreboard scoreboard = player.getWorldScoreboard();
        //noinspection ConstantConditions
        if (scoreboard == null) return null;

        final ScoreObjective objective = getObjective(name, scoreboard, criteria);
        return getOrCreateScore(player, name, initialValue, scoreboard, objective);
    }

    /**
     * Gets or creates the score for the player
     * @param player        The player
     * @param name          The name of the score
     * @param initialValue  The initial value if created
     * @param scoreboard    The scoreboard to get the objective from
     * @param objective     The objective to get
     * @return The {@link Score}
     */
    public static Score getOrCreateScore(final PlayerEntity player, final String name, final double initialValue, final Scoreboard scoreboard, final ScoreObjective objective) {
        Score score = scoreboard.getObjectivesForEntity(player.getScoreboardName()).get(objective);

        // If the score didn't exist:
        if (score == null) {
            try {
                score = scoreboard.getOrCreateScore(player.getScoreboardName(), objective);
                score.setScorePoints((int) Math.round(initialValue));
            } catch (final IllegalArgumentException e) {
                // gg Mojang; this will blow up due to overly long UNs, but the other method won't.
                NeedsMod.LOGGER.error("Unable to create scoreboard objective for '" + name + "'.", e);
            }
        }

        return score;
    }

    /**
     * Gets or creates an objective on the given scoreboard
     * @param name       The name of the objective
     * @param scoreboard The scoreboard
     * @param criteria   The criterion to use for the objective if created
     * @return The {@link ScoreObjective}
     */
    public static ScoreObjective getObjective(final String name, final Scoreboard scoreboard, final ScoreCriteria criteria) {
        ScoreObjective objective = scoreboard.getOrCreateObjective(name);

        // No, it really can be null...
        //noinspection ConstantConditions
        if (objective != null) return objective;

        objective = scoreboard.addObjective(
                name, // The internal name
                criteria, // The criteria type
                new StringTextComponent(name), // The display name
                ScoreCriteria.RenderType.INTEGER // How to display it
        );
        return objective;
    }

    /**
     * Gets a criterion by name
     * @param criterion The criterion name
     * @return The {@link ScoreCriteria}
     */
    public static ScoreCriteria getCriterion(final String criterion) {
        /*
            There's a still obfuscated method in ScoreCriteria: Optional<ScoreCriteria> func_216390_a(String p_216390_0_)
            which seems to do this.. better? Though better may be a subjective term. However, this may not properly
            track item usage as-is.
            TODO: Determine if this tracks item usage or if we need to do the stat malarky.
         */
        final ScoreCriteria instance = ScoreCriteria.INSTANCES.get(criterion);
        return instance != null ? instance : new ScoreCriteria(criterion);
    }
}

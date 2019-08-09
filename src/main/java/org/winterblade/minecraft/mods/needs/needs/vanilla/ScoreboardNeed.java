package org.winterblade.minecraft.mods.needs.needs.vanilla;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.CachedTickingNeed;
import org.winterblade.minecraft.mods.needs.mixins.ScoreboardMixin;
import org.winterblade.minecraft.mods.needs.util.ScoreHelper;

import java.util.Map;
import java.util.WeakHashMap;

@Document(description = "Used to read in scoreboard values of the given criterion; this can be specified multiple times, with different criteria for each one")
public class ScoreboardNeed extends CachedTickingNeed {
    @Expose
    @Document(description = "The name of the need and the scoreboard entry; needs to be unique.")
    protected String name;

    @Expose
    @Document(description = "The scoreboard criterion; see https://minecraft.gamepedia.com/Scoreboard for valid criteria")
    protected String criterion;

    @Expose
    @Document(description = "The default value of the score")
    @OptionalField(defaultValue = "0")
    protected int defaultValue = 0;

    private final Map<PlayerEntity, Score> scoreCache = new WeakHashMap<>();

    private Scoreboard scoreboard;
    private ScoreObjective objective;
    private boolean readOnly;

    @Override
    public void onLoaded() {
        if (getMixins().stream().anyMatch((m) -> m instanceof ScoreboardMixin)) {
            throw new IllegalArgumentException("Scoreboard need cannot have Scoreboard mixin applied to it.");
        }
        super.onLoaded();
    }

    @Override
    public boolean allowMultiple() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return Integer.MIN_VALUE;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return Integer.MAX_VALUE;
    }

    @Override
    public double getValue(final PlayerEntity player) {
        return getScore(player)
                .getScorePoints();
    }


    @Override
    public boolean isValueInitialized(final PlayerEntity player) {
        return scoreboard != null
                && objective != null
                && scoreboard.getObjectivesForEntity(player.getScoreboardName()).containsKey(objective);
    }

    @Override
    protected double setValue(final PlayerEntity player, final double newValue, final double adjustAmount) {
        final Score score = getScore(player);

        if (readOnly) return getValue(player);
        score.setScorePoints((int) newValue);

        return score.getScorePoints();
    }

    private Score getScore(final PlayerEntity player) {
        return scoreCache
            .computeIfAbsent(player,
                (p) -> ScoreHelper.getOrCreateScore(player, name, defaultValue, getScoreboard(player), getObjective(player)));
    }

    private Scoreboard getScoreboard(final PlayerEntity player) {
        if (scoreboard != null) return scoreboard;
        scoreboard = player.getWorldScoreboard();
        return scoreboard;
    }

    private ScoreObjective getObjective(final PlayerEntity player) {
        if (objective != null) return objective;
        final ScoreCriteria criterion = ScoreHelper.getCriterion(this.criterion);
        readOnly = criterion.isReadOnly();
        objective = ScoreHelper.getObjective(name, getScoreboard(player), criterion);
        return objective;
    }
}

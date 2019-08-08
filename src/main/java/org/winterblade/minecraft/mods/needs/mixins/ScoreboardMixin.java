package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraft.scoreboard.Score;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedInitializationEvent;
import org.winterblade.minecraft.mods.needs.util.ScoreHelper;

@Document(description = "Use this mixin to mirror the need to a scoreboard entry of the same name as the need; the " +
        "value of the need will be rounded to the nearest whole number.")
public class ScoreboardMixin extends BaseMixin {
    @SubscribeEvent
    public void onInitialized(final NeedInitializationEvent.Post evt) {
        if (evt.getNeed() != need) return;
        ScoreHelper.getOrCreateScore(evt.getPlayer(), evt.getNeed().getName(), evt.getNeed().getValue(evt.getPlayer()));
    }

    @SubscribeEvent
    public void onAdjusted(final NeedAdjustmentEvent.Post evt) {
        if (evt.getNeed() != need) return;

        final Score score = ScoreHelper.getOrCreateScore(evt.getPlayer(), evt.getNeed().getName(), evt.getCurrent());
        if (score == null) return;

        score.setScorePoints((int) Math.round(evt.getCurrent()));
    }
}

package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

@Document(description = "Use this mixin to print every, single, individual, precise change to the player's chat. I don't " +
        "know why you would use it, but, it's here in case u li3k ann0y1ng people.")
public class ChatMixin extends BaseMixin {
    @SubscribeEvent
    public void onAdjusted(final NeedAdjustmentEvent.Post evt) {
        if (evt.getNeed() != need) return;

        // Send a message about it:
        final double amount = evt.getCurrent() - evt.getPrevious();
        final NeedLevel level = evt.getNeed().getLevel(evt.getCurrent());
        evt.getPlayer().sendStatusMessage(
            new StringTextComponent(
                    String.format(
                            "Your %s has %s by %.2f to %.2f; you're now %s.",
                            evt.getNeed().getName().toLowerCase(),
                            amount < 0 ? "decreased" : "increased",
                            Math.abs(amount),
                            evt.getCurrent(),
                            !level.equals(NeedLevel.UNDEFINED) ? level.getName().toLowerCase() : "neutral"
                )
            ), false);
    }
}

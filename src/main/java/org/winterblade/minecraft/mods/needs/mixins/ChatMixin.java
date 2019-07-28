package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;

public class ChatMixin extends BaseMixin {
    @SubscribeEvent
    public void onAdjusted(NeedAdjustmentEvent.Post evt) {
        if (evt.getNeed() != need) return;

        // Send a message about it:
        evt.getPlayer().sendStatusMessage(
            new StringTextComponent(
                evt.getSource().formatMessage(
                    evt.getNeed().getName(),
                    evt.getCurrent() - evt.getPrevious(),
                    evt.getCurrent(),
                    evt.getNeed().getLevel(evt.getCurrent())
                )
            ), false);
    }
}

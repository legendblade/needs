package org.winterblade.minecraft.mods.needs.mixins;

import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.events.NeedAdjustmentEvent;

@Mod.EventBusSubscriber
public class ChatMixin extends BaseMixin {
    public ChatMixin(Need need) {
        super(need);
    }

    @SubscribeEvent
    public void onAdjusted(NeedAdjustmentEvent.Post evt) {
        if (evt.getNeed() != need) return;

        // Send a message about it:
        evt.getPlayer().sendStatusMessage(
            new StringTextComponent(
                evt.getSource().FormatMessage(
                    evt.getNeed().getName(),
                    evt.getCurrent() - evt.getPrevious(),
                    evt.getCurrent()
                )
            ), false);
    }
}

package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.events.NeedLevelEvent;

public class ChatLevelMixin extends BaseMixin {
    @Expose
    private boolean actionBar;

    @SubscribeEvent
    public void onAdjusted(NeedLevelEvent.Changed evt) {
        if (evt.getNeed() != need) return;

        // Send a message about it:
        evt.getPlayer().sendStatusMessage(
                new StringTextComponent(
                    String.format(
                        "Your \u00a73%s\u00a7r has changed from \u00a73%s\u00a7r to \u00a73%s\u00a7r",
                        need.getName(),
                        evt.getPreviousLevel().getName(),
                        evt.getLevel().getName()
                    )
                ), actionBar);
    }
}

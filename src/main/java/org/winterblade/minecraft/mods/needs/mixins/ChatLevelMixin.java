package org.winterblade.minecraft.mods.needs.mixins;

import com.google.gson.annotations.Expose;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.events.NeedLevelEvent;

@Document(description = "Display a notification in chat anytime the need changes from one level to another")
public class ChatLevelMixin extends BaseMixin {
    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If set to true, display this value in the action bar. If not, it will be shown in the player's chat.")
    private boolean actionBar;

    @SubscribeEvent
    public void onAdjusted(final NeedLevelEvent.Changed evt) {
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

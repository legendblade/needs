package org.winterblade.minecraft.mods.needs.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.registries.NeedRegistry;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.components.*;

import java.util.List;

public class NeedDisplayScreen extends ComponentScreen {
    private NeedDisplayScreen() {
        super(new StringTextComponent(""), 318, 227);

        texture = new Texture(NeedsMod.MODID, "need_ui", 384, 384);
        window = new WindowComponent(texture.getSubtexture(guiWidth, guiHeight), guiWidth, guiHeight);

        // Components
        window.addComponent(new TextComponent("Needs, Wants, and Desires", 7, 7, 0xFFFFFF, true));

        final int itemHeight = 39;
        final ScrollbarComponent bar = new ScrollbarComponent(
            new Rectangle2d(298, 24, 12, 195),
            texture.getSubtexture(12, 15, 318, 0),
            texture.getSubtexture(12, 15, 330, 0),
            () -> (NeedRegistry.INSTANCE.getLocalNeeds().size() * itemHeight) - 195
        );
        window.addComponent(bar);
        window.addComponent(new ScrollpaneComponent<>(
                bar,
                new Rectangle2d(8, 24, 284, 195),
                (i) -> new NeedComponent(texture.getSubtexture(284, itemHeight, 0, 227), new Rectangle2d(0, 0, 284, itemHeight)),
                itemHeight,
                (c, i) -> {
                    final List<Need.Local> localNeeds = NeedRegistry.INSTANCE.getLocalNeeds();
                    if (localNeeds.size() <= i) {
                        c.setVisible(false);
                        return;
                    }

                    final Need.Local localNeed = localNeeds.get(i);
                    final Need need = localNeed.getNeed().get();
                    if (need == null) {
                        c.setVisible(false);
                        return;
                    }

                    c.setVisible(true);
                    c.setTitle(localNeed.getName());
                    c.setBarValues(localNeed.getMin(), localNeed.getMax(), localNeed.getValue(), (i * 0x66) % 0xFFFFFF);
                }
        ));
    }

    public static void open() {
        Minecraft.getInstance().displayGuiScreen(new NeedDisplayScreen());
    }
}

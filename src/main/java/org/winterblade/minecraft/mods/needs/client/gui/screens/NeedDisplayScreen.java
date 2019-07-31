package org.winterblade.minecraft.mods.needs.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.components.*;

public class NeedDisplayScreen extends ComponentScreen {
    private NeedDisplayScreen() {
        super(new StringTextComponent(""), 318, 227);

        texture = new Texture(NeedsMod.MODID, "need_ui", 384, 384);
        window = new WindowComponent(texture.getSubtexture(guiWidth, guiHeight), guiWidth, guiHeight);

        // Components
        window.addComponent(new TextComponent("Needs, Wants, and Desires", 7, 7, 0xFFFFFF, true));
        window.addComponent(new ScrollpaneComponent(
                new ScrollbarComponent(new Rectangle2d(0, 0, 0, 0)),
                new Rectangle2d(8, 24, 284, 195),
                (i) -> new NeedComponent(texture.getSubtexture(284, 39, 0, 227), new Rectangle2d(0, 0, 284, 39)),
                39,
                (c, i) -> {}
        ));
    }

    public static void open() {
        Minecraft.getInstance().displayGuiScreen(new NeedDisplayScreen());
    }
}

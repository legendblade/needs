package org.winterblade.minecraft.mods.needs.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;
import org.winterblade.minecraft.mods.needs.client.gui.components.*;

import javax.annotation.Nonnull;

public class NeedDisplayScreen extends Screen {
    protected Texture texture = new Texture(NeedsMod.MODID, "need_ui", 384, 384);
    private int guiLeft;
    private int guiTop;
    private final int guiWidth = 318;
    private final int guiHeight = 227;

    private ComponentBase background;

    protected NeedDisplayScreen() {
        super(new StringTextComponent(""));
    }

    @Override
    protected void init() {
        guiLeft = (width - guiWidth)/2;
        guiTop = (height - guiHeight)/2;

        // Components
        background = new WindowComponent(texture.getSubtexture(guiWidth, guiHeight));
        background.addComponent(new TextComponent("Needs, Wants, and Desires", 0xFFFFFF, true), 7, 7);
        background.addComponent(new ScrollpaneComponent(
                new ScrollbarComponent(),
                284,
                195,
                (i) -> new NeedComponent(texture.getSubtexture(284, 39, 0, 227)),
                39,
                (c, i) -> {}
        ), 8, 24);

        super.init();
    }

    @Nonnull
    @Override
    public String getNarrationMessage() {
        // TODO: Read out needs list?
        return super.getNarrationMessage();
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        background.draw(guiLeft, guiTop);
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open() {
        Minecraft.getInstance().displayGuiScreen(new NeedDisplayScreen());
    }
}

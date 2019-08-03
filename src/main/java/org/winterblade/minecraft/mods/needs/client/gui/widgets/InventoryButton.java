package org.winterblade.minecraft.mods.needs.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import javax.annotation.Nonnull;

public class InventoryButton extends ImageButton {
    private static final IPressable action = new Action();

    @SuppressWarnings("WeakerAccess")
    public InventoryButton(final int x, final int y) {
        super(
                x, // X
                y, // Y
            20, // Height
            20, // Width
            305, // xTexStart
            222, // yTexStart
            20, // Hover state yOffset
            new ResourceLocation(NeedsMod.MODID, "textures/gui/need_ui.png"),
            384, // Texture width
            384, // Texture height
            action, // Action
            "" // Narration message
        );
    }

    private static class Action implements IPressable {
        @Override
        public void onPress(@Nonnull final Button btn) {
            Minecraft.getInstance().displayGuiScreen(new InventoryScreen(Minecraft.getInstance().player));
        }
    }
}

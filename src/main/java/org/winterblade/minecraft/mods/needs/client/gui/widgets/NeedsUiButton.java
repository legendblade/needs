package org.winterblade.minecraft.mods.needs.client.gui.widgets;

import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.client.gui.screens.NeedDisplayScreen;
import org.winterblade.minecraft.mods.needs.mixins.UiMixin;

import javax.annotation.Nonnull;

public class NeedsUiButton extends ImageButton {
    private static final IPressable action = new Action();

    @SuppressWarnings("WeakerAccess")
    public NeedsUiButton(final int x, final int y) {
        super(
                x, // X
                y, // Y
            20, // Height
            20, // Width
            285, // xTexStart
            227, // yTexStart
            20, // Hover state yOffset
            new ResourceLocation(NeedsMod.MODID, "textures/gui/need_ui.png"),
            384, // Texture width
            384, // Texture height
            action, // Action
            "" // Narration message
        );
    }

    /**
     * Event for adding the UI button
     * @param event The event
     */
    public static void onUiOpened(final GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof InventoryScreen) || UiMixin.getLocalNeeds().size() <= 0) return;

        // TODO: also config options for button display/styling
        final InventoryScreen ui = (InventoryScreen) event.getGui();
        event.addWidget(new NeedsUiButton(ui.getGuiLeft() + 177, ui.getGuiTop() + 1));
    }

    private static class Action implements IPressable {
        @Override
        public void onPress(@Nonnull final Button btn) {
            NeedDisplayScreen.open();
        }
    }
}

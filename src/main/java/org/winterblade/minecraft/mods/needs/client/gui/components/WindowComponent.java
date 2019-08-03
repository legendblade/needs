package org.winterblade.minecraft.mods.needs.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.winterblade.minecraft.mods.needs.client.gui.Texture;

public class WindowComponent extends TexturedComponent {

    private final MainWindow mainWindow = Minecraft.getInstance().mainWindow;

    public WindowComponent(final Texture texture, final Rectangle2d bounds) {
        super(texture, bounds);
    }

    public WindowComponent(final Texture texture, final int width, final int height) {
        this(texture, new Rectangle2d(0, 0, width, height));
    }

    @Override
    public void draw(final int x, final int y) {
        if (!isVisible()) return;
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiUtils.drawGradientRect(-50, 0, 0, mainWindow.getWidth(), mainWindow.getHeight(), -1072689136, -804253680);
        super.draw(x, y);
    }
}

package org.winterblade.minecraft.mods.needs.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;

public abstract class GuiLib {
    /**
     * Offsets the screen, applies the given angle rotation, and then runs the draw function before rotating it
     * and shifting it all back.
     * @param angle The angle of rotation, in degrees
     * @param x     The x position to rotate around
     * @param y     The y position to rotate around
     * @param draw  The callback to do the drawing
     */
    public static void rotateAndDraw(final double angle, final double x, final double y, final Runnable draw) {
        final double z = -1;

        final double radians = Math.toRadians(angle);
        final double x1 = x - ((x * Math.cos(radians)) - (y * Math.sin(radians)));
        final double y1 = y - ((x * Math.sin(radians)) + (y * Math.cos(radians)));

        GlStateManager.disableDepthTest();
        GlStateManager.rotated( angle,  0, 0, z);
        GlStateManager.translated(-x1, -y1, 0);
        draw.run();
        GlStateManager.translated(x1, y1, 0);
        GlStateManager.rotated(-angle, 0, 0, z);
    }
}

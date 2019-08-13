package org.winterblade.minecraft.mods.needs.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public class OverlayRenderer extends AbstractGui {
    private final Minecraft mc;
    private final Supplier<ResourceLocation> resource;
    private final double width;
    private final double height;
    private final double x;
    private final double y;
    private ResourceLocation overlay;

    public OverlayRenderer(final Supplier<ResourceLocation> resource, final double width, final double height, final double x, final double y) {
        this.resource = resource;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        mc = Minecraft.getInstance();
    }

    public void render() {
        if (overlay == null) overlay = resource.get();

        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);

        final double z = -90;

        final double y = this.y < 0 ? mc.mainWindow.getScaledHeight() + this.y : this.y;
        final double x = this.x < 0 ? mc.mainWindow.getScaledWidth() + this.x : this.x;

        final double y2 = (height <= 1 ? height * mc.mainWindow.getScaledHeight() : height) + y;
        final double x2 = (width <= 1 ? width * mc.mainWindow.getScaledWidth() : width) + x;

        final double u1 = 0;
        final double u2 = 1;
        final double v1 = 0;
        final double v2 = 1;

        this.mc.getTextureManager().bindTexture(overlay);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos( x, y2, z).tex(u1, v2).endVertex();
        bufferbuilder.pos(x2, y2, z).tex(u2, v2).endVertex();
        bufferbuilder.pos(x2,  y, z).tex(u2, v1).endVertex();
        bufferbuilder.pos( x,  y, z).tex(u1, v1).endVertex();
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
        // This is at the end of the MC vignette function.
        // If we change this above, we need to ensure it goes back to the proper(?) state at the end here:
//        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
//        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }
}

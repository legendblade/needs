package org.winterblade.minecraft.mods.needs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Texture {
    private ResourceLocation loc;
    private Supplier<ResourceLocation> getLoc;
    private final int width;
    private final int height;
    private final int drawWidth;
    private final int drawHeight;

    private float u1;
    private float u2;
    private float v1;
    private float v2;

    /**
     * Create a 256x256 texture resource
     * @param loc The location for the resource
     */
    public Texture(final ResourceLocation loc) {
        this(loc, 256, 256);
    }

    /**
     * Create a texture of the given size
     * @param mod    The mod's ID
     * @param file   The filename; this is assumed to be in textures/gui/{file}.png
     * @param width  The width of the texture
     * @param height The height of the texture
     */
    public Texture(final String mod, final String file, final int width, final int height) {
        this(new ResourceLocation(mod, "textures/gui/" + file + ".png"), width, height);
    }

    /**
     * Create a width*height texture resource
     * @param loc    The location of the texture
     * @param width  The width of the texture
     * @param height The height of the texture
     */
    public Texture(final ResourceLocation loc, final int width, final int height) {
        this(loc, width, height, 0, 0, width, height);
    }

    /**
     * Creates a texture at the given location, with the given offsets
     * @param loc        The location of the texture
     * @param texWidth   The width of the texture
     * @param texHeight  The height of the texture
     * @param xOffset    The X offset of the texture in the texture PNG
     * @param yOffset    The Y offset of the texture in the texture PNG
     * @param drawWidth  The width to draw the texture
     * @param drawHeight The height to draw the texture
     */
    public Texture(final ResourceLocation loc, final int texWidth, final int texHeight, final int xOffset,
                   final int yOffset, final int drawWidth, final int drawHeight) {
        this.loc = loc;
        this.width = texWidth;
        this.height = texHeight;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;

        // Store props for blit
        this.updateXOffset(xOffset);
        this.updateYOffset(yOffset);
    }

    /**
     * Creates a texture at the given location, with the given offsets
     * @param getLoc     A supplier to get the location
     * @param texWidth   The width of the texture
     * @param texHeight  The height of the texture
     * @param xOffset    The X offset of the texture in the texture PNG
     * @param yOffset    The Y offset of the texture in the texture PNG
     * @param drawWidth  The width to draw the texture
     * @param drawHeight The height to draw the texture
     */
    public Texture(final Supplier<ResourceLocation> getLoc, final int texWidth, final int texHeight, final int xOffset,
                   final int yOffset, final int drawWidth, final int drawHeight) {
        this.getLoc = getLoc;
        this.width = texWidth;
        this.height = texHeight;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;

        // Store props for blit
        this.updateXOffset(xOffset);
        this.updateYOffset(yOffset);
    }

    /**
     * Binds the texture to the texture manager
     */
    public void bind() {
        if (loc == null) {
            loc = getLoc.get();
            if (loc == null) loc = new ResourceLocation("minecraft", "givemeamissingtexturepls");
        }
        Minecraft.getInstance().getTextureManager().bindTexture(loc);
    }

    /**
     * Gets a subtexture of this texture
     * @param width   The subtexture's width
     * @param height  The subtexture's height
     * @return  The new texture
     */
    public Texture getSubtexture(final int width, final int height) {
        return getSubtexture(width, height, 0, 0);
    }

    /**
     * Gets a subtexture of this texture
     * @param width   The subtexture's width
     * @param height  The subtexture's height
     * @param xOffset The x offset in the texture file
     * @param yOffset The y offset in the texture file
     * @return  The new texture
     */
    public Texture getSubtexture(final int width, final int height, final int xOffset, final int yOffset) {
        if (this.loc != null) return new Texture(this.loc, this.width, this.height, xOffset, yOffset, width, height);
        return new Texture(this.getLoc, this.width, this.height, xOffset, yOffset, width, height);
    }

    /**
     * Draw the texture at the given coordinates
     * @param x The left
     * @param y The top
     * @param z The z-depth of the texture
     */
    public void draw(final int x, final int y, final int z) {
        final int x2 = x + drawWidth;
        final int y2 = y + drawHeight;

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos( x, y2, z).tex(u1, v2).endVertex();
        bufferbuilder.pos(x2, y2, z).tex(u2, v2).endVertex();
        bufferbuilder.pos(x2,  y, z).tex(u2, v1).endVertex();
        bufferbuilder.pos( x,  y, z).tex(u1, v1).endVertex();
        tessellator.draw();
    }

    public int getDrawWidth() {
        return drawWidth;
    }

    public int getDrawHeight() {
        return drawHeight;
    }

    public void updateYOffset(final double yOffset) {
        this.v1 = (float) ((yOffset + 0F) / (float)height);
        this.v2 = (float) ((yOffset + (float)drawHeight) / (float)height);
    }

    public void updateXOffset(final double xOffset) {
        this.u1 = (float) ((xOffset + 0F) / (float)width);
        this.u2 = (float) ((xOffset + (float)drawWidth) / (float)width);
    }
}
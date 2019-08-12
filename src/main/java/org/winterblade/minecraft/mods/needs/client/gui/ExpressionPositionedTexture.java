package org.winterblade.minecraft.mods.needs.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;

import java.util.function.Supplier;

public class ExpressionPositionedTexture extends Texture {
    private final ExpressionContext yOffsetExpression;
    private final ExpressionContext xOffsetExpression;
    private final boolean isConstant;

    public ExpressionPositionedTexture(final ResourceLocation loc, final int texWidth, final int texHeight,
                                       final ExpressionContext xOffsetExpression, final ExpressionContext yOffsetExpression,
                                       final int drawWidth, final int drawHeight) {
        super(loc, texWidth, texHeight, 0, 0, drawWidth, drawHeight);

        this.yOffsetExpression = yOffsetExpression;
        this.xOffsetExpression = xOffsetExpression;
        isConstant = yOffsetExpression.isConstant() && xOffsetExpression.isConstant();
    }

    public ExpressionPositionedTexture(final Supplier<ResourceLocation> loc, final int texWidth, final int texHeight,
                                       final ExpressionContext xOffsetExpression, final ExpressionContext yOffsetExpression,
                                       final int drawWidth, final int drawHeight) {
        super(loc, texWidth, texHeight, 0, 0, drawWidth, drawHeight);

        this.yOffsetExpression = yOffsetExpression;
        this.xOffsetExpression = xOffsetExpression;
        isConstant = yOffsetExpression.isConstant() && xOffsetExpression.isConstant();
    }

    public ExpressionPositionedTexture(final ResourceLocation location, final int texWidth, final int texHeight) {
        super(location, texWidth, texHeight);
        yOffsetExpression = null;
        xOffsetExpression = null;
        isConstant = true;
    }

    public void setAndRecalculate(final String var, final Supplier<Double> val) {
        if (isConstant) return;
        if (yOffsetExpression != null) updateYOffset(yOffsetExpression.setIfRequired(var, val).apply(Minecraft.getInstance().player));
        if (xOffsetExpression != null) updateXOffset(xOffsetExpression.setIfRequired(var, val).apply(Minecraft.getInstance().player));
    }
}

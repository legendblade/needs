package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.expressions.DamageExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

public abstract class DamageBasedManipulator extends BaseManipulator {
    @Expose
    protected DamageExpressionContext amount;

    @Expose
    protected boolean hostile = true;

    @Expose
    protected boolean passive = true;

    @Expose
    protected boolean players = true;

    @Expose
    protected double minAmount = Double.NEGATIVE_INFINITY;

    @Expose
    protected double maxAmount = Double.POSITIVE_INFINITY;
}

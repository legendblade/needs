package org.winterblade.minecraft.mods.needs.api;

import java.util.function.Supplier;

public interface IExpression extends Supplier<Double> {
    IExpression setIfRequired(String arg, Supplier<Double> value);

    boolean isRequired(String arg);
}

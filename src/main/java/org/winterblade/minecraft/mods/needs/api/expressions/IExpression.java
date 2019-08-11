package org.winterblade.minecraft.mods.needs.api.expressions;

import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.api.needs.LazyNeed;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IExpression extends Function<PlayerEntity, Double> {
    IExpression setIfRequired(String arg, Supplier<Double> value);

    boolean isRequired(String arg);

    List<LazyNeed> getNeeds();
}

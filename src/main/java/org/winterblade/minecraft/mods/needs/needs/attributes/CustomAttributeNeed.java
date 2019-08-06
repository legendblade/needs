package org.winterblade.minecraft.mods.needs.needs.attributes;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public abstract class CustomAttributeNeed extends AttributeBasedNeed {
    @Expose
    protected double min = Double.NEGATIVE_INFINITY;

    @Expose
    protected double max = Double.POSITIVE_INFINITY;

    protected final double minFromAttribute;
    protected final double maxFromAttribute;

    public CustomAttributeNeed(final IAttribute attribute, final UUID modifierId, final String modifierName, final double minFromAttribute, final double maxFromAttribute) {
        super(attribute, modifierId, modifierName);
        this.minFromAttribute = minFromAttribute;
        this.maxFromAttribute = maxFromAttribute;
    }

    @Override
    public void onCreated() {
        if (min < minFromAttribute) {
            NeedsMod.LOGGER.warn("Minimum value of " + getName() + " need is less than the value of the attribute it relies on: " + minFromAttribute);
            min = minFromAttribute;
        }

        if (maxFromAttribute < max) {
            NeedsMod.LOGGER.warn("Maximum value of " + getName() + " need is larger than the value of the attribute it relies on: " + maxFromAttribute);
            max = maxFromAttribute;
        }

        // TODO: Is this necessary? Does it break things? Who knows
        if (!attribute.getShouldWatch() && attribute instanceof Attribute) {
            NeedsMod.LOGGER.info("Setting " + attribute.getName() + " to be watched.");
            ((Attribute) attribute).setShouldWatch(true);
        }

        super.onCreated();
    }

    @Override
    public double getMin(final PlayerEntity player) {
        return min;
    }

    @Override
    public double getMax(final PlayerEntity player) {
        return max;
    }
}

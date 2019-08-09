package org.winterblade.minecraft.mods.needs.needs.attributes;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.PlayerEntity;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;

import java.util.UUID;

@SuppressWarnings("WeakerAccess")
@Document(description = "A collection of attribute-based needs that use custom modifiers to affect the given stat")
public abstract class CustomAttributeNeed extends AttributeBasedNeed {
    @Expose
    @OptionalField(defaultValue = "Minimum of the attribute")
    @Document(description = "The minimum value this need can be set through the the mod; this can be higher than the " +
            "actual minimum value of the attribute")
    protected double min = Double.NEGATIVE_INFINITY;

    @Expose
    @OptionalField(defaultValue = "Maximum of the attribute")
    @Document(description = "The maximum value this need can be set through the the mod; this can be lower than the " +
            "actual maximum value of the attribute")
    protected double max = Double.POSITIVE_INFINITY;

    protected final double minFromAttribute;
    protected final double maxFromAttribute;

    public CustomAttributeNeed(final IAttribute attribute, final UUID modifierId, final String modifierName, final double minFromAttribute, final double maxFromAttribute) {
        super(attribute, modifierId, modifierName);
        this.minFromAttribute = minFromAttribute;
        this.maxFromAttribute = maxFromAttribute;
    }

    @Override
    public void onLoaded() {
        if (min == Double.NEGATIVE_INFINITY) min = minFromAttribute;
        else if (min < minFromAttribute) {
            NeedsMod.LOGGER.warn("Minimum value of " + getName() + " need is less than the value of the attribute it relies on: " + minFromAttribute);
            min = minFromAttribute;
        }

        if (max == Double.POSITIVE_INFINITY) max = maxFromAttribute;
        else if (maxFromAttribute < max) {
            NeedsMod.LOGGER.warn("Maximum value of " + getName() + " need is larger than the value of the attribute it relies on: " + maxFromAttribute);
            max = maxFromAttribute;
        }

        // TODO: Is this necessary? Does it break things? Who knows
        if (!attribute.getShouldWatch() && attribute instanceof Attribute) {
            NeedsMod.LOGGER.info("Setting " + attribute.getName() + " to be watched.");
            ((Attribute) attribute).setShouldWatch(true);
        }

        super.onLoaded();
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

package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

@SuppressWarnings("WeakerAccess")
@Document(description = "The base for most manipulators")
public abstract class BaseManipulator implements IManipulator {
    /**
     * Used in events anytime a Need is changed by an external source.
     */
    public static IManipulator EXTERNAL = new ExternalManipulator();

    protected Need parent;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "If specified, the lowest value this manipulator can set the parent need to; if set, this " +
            "will incur a slight performance hit, as the value of the need has to be calculated at the manipulator level.")
    protected double downTo = Double.NEGATIVE_INFINITY;

    @Expose
    @OptionalField(defaultValue = "None")
    @Document(description = "If specified, the highest value this manipulator can set the parent need to; if set, this " +
            "will incur a slight performance hit, as the value of the need has to be calculated at the manipulator level.")
    protected double upTo = Double.POSITIVE_INFINITY;

    @Override
    public final void onLoaded(final Need need) {
        parent = need;
        onLoaded();
    }

    /**
     * Used to finish up any necessary post creation tasks
     */
    public void onLoaded() {}

    private static class ExternalManipulator extends BaseManipulator {

    }

    @Override
    public double getLowestToSetNeed() {
        return downTo;
    }

    @Override
    public double getHighestToSetNeed() {
        return upTo;
    }
}

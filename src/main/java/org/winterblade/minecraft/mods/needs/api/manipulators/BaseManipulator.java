package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

@SuppressWarnings("WeakerAccess")
public abstract class BaseManipulator implements IManipulator {
    /**
     * Used in events anytime a Need is changed by an external source.
     */
    public static IManipulator EXTERNAL = new ExternalManipulator();

    protected Need parent;

    @Expose
    protected String messageFormat;

    @Override
    public final void onCreated(final Need need) {
        parent = need;
        onCreated();
    }

    /**
     * Used to finish up any necessary post creation tasks
     */
    public void onCreated() {}

    @Override
    public String formatMessage(final String needName, final double amount, final double newValue, final NeedLevel level) {
        return String.format(
                messageFormat != null ? messageFormat : "Your %s has %s by %.2f to %.2f; you're now %s.",
                needName.toLowerCase(),
                amount < 0 ? "decreased" : "increased",
                Math.abs(amount),
                newValue,
                !level.equals(NeedLevel.UNDEFINED) ? level.getName().toLowerCase() : "neutral"
        );
    }

    private static class ExternalManipulator extends BaseManipulator {

    }
}

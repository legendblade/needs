package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.Need;

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
    public final void onCreated(Need need) {
        parent = need;
        onCreated();
    }

    /**
     * Used to finish up any necessary post creation tasks
     */
    public void onCreated() {}

    @Override
    public String formatMessage(String needName, double amount, double newValue) {
        return String.format(
                messageFormat != null ? messageFormat : "Your %s has %s by %.2f to %.2f",
                needName.toLowerCase(),
                amount < 0 ? "decreased" : "increased",
                Math.abs(amount),
                newValue
        );
    }

    private static class ExternalManipulator extends BaseManipulator {

    }
}

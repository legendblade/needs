package org.winterblade.minecraft.mods.needs.api.manipulators;

import com.google.gson.annotations.Expose;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedManipulator;

public abstract class BaseManipulator implements IManipulator {
    protected Need parent;

    @Expose
    protected String messageFormat;

    @Override
    public final void OnCreated(Need need) {
        parent = need;
        OnCreated();
    }

    public abstract void OnCreated();

    @Override
    public String FormatMessage(String needName, int amount, int newValue) {
        return String.format(
                messageFormat != null ? messageFormat : "Your %s has %s by %d to %d",
                needName.toLowerCase(),
                amount < 0 ? "decreased" : "increased",
                Math.abs(amount),
                newValue
        );
    }

    protected void CopyFrom(ItemUsedManipulator other) {
        messageFormat = other.messageFormat;
    }
}

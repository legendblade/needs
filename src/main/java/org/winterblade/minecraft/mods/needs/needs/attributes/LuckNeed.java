package org.winterblade.minecraft.mods.needs.needs.attributes;

import net.minecraft.entity.SharedMonsterAttributes;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.util.UUID;

public class LuckNeed extends CustomAttributeNeed {
    private static final UUID modifierId = UUID.fromString("e1395974-4e13-4451-8f52-ec7f93562ebc");
    private static final String modifierName = NeedsMod.MODID + ".LuckNeed";

    public LuckNeed() {
        super(SharedMonsterAttributes.LUCK, modifierId, modifierName, -1024, 1024);
    }

    @Override
    public String getName() {
        return "Luck";
    }
}

package org.winterblade.minecraft.mods.needs.needs.attributes;

import net.minecraft.entity.SharedMonsterAttributes;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;

import java.util.UUID;

@Document(description = "Tracks the player's current attack damage bonus")
public class AttackDamageNeed extends CustomAttributeNeed {
    private static final UUID attackDamageModifier = UUID.fromString("caafae10-7bad-419a-b3f9-a460e5f42dd1");
    private static final String attackDamageModifierName = NeedsMod.MODID + ".AttackDamageNeed";

    protected AttackDamageNeed() {
        super(SharedMonsterAttributes.ATTACK_DAMAGE, attackDamageModifier, attackDamageModifierName, 0, 2048);
    }

    @Override
    public String getName() {
        return "Attack Damage";
    }
}

package org.winterblade.minecraft.mods.needs.actions;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.Need;
import org.winterblade.minecraft.mods.needs.api.actions.IReappliedOnDeathLevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

import javax.annotation.Nullable;

public class PotionEffectLevelAction extends LevelAction implements IReappliedOnDeathLevelAction {
    @Expose
    private String effect;

    @Expose
    private int duration;

    @Expose
    private int amplifier;

    @Expose
    private boolean persistOnDeath;

    private Effect theEffect;

    @Override
    public String getName() {
        return "Potion";
    }

    @Nullable
    private Effect getTheEffect() {
        if (theEffect != null) return theEffect;

        theEffect = RegistryManager.ACTIVE.getRegistry(Effect.class).getValue(new ResourceLocation(effect));

        if (theEffect == null) {
            NeedsMod.LOGGER.warn("Unable to get potion effect '" + effect + "'.");
        }

        return theEffect;
    }

    @Override
    public void onEntered(Need need, NeedLevel level, PlayerEntity player) {
        Effect eff = getTheEffect();
        if (eff == null) return;

        player.addPotionEffect(new EffectInstance(eff, duration, amplifier));
    }

    @Override
    public void onExited(Need need, NeedLevel level, PlayerEntity player) {
        onEntered(need, level, player);
    }

    @Override
    public void onContinuousStart(Need need, NeedLevel level, PlayerEntity player) {
        Effect eff = getTheEffect();
        if (eff == null) return;

        player.addPotionEffect(new EffectInstance(eff, Integer.MAX_VALUE, amplifier));
    }

    @Override
    public void onContinuousEnd(Need need, NeedLevel level, PlayerEntity player) {
        Effect eff = getTheEffect();
        if (eff == null) return;

        player.removePotionEffect(eff);
    }

    @Override
    public void onRespawned(Need need, NeedLevel level, PlayerEntity player, PlayerEntity oldPlayer) {
        if (!persistOnDeath) return;

        Effect eff = getTheEffect();
        if (eff == null) return;

        EffectInstance effectInstance = oldPlayer.getActivePotionEffect(eff);
        if (effectInstance != null) player.addPotionEffect(effectInstance);
    }

    @Override
    public void onRespawnedWhenContinuous(Need need, NeedLevel level, PlayerEntity player, PlayerEntity oldPlayer) {
        onContinuousStart(need, level, player);
    }
}

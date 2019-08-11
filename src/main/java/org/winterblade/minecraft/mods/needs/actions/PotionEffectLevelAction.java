package org.winterblade.minecraft.mods.needs.actions;

import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.api.actions.IReappliedOnDeathLevelAction;
import org.winterblade.minecraft.mods.needs.api.actions.LevelAction;
import org.winterblade.minecraft.mods.needs.api.levels.NeedLevel;

import javax.annotation.Nullable;

@Document(description = "Applies a potion effect to the player")
public class PotionEffectLevelAction extends LevelAction implements IReappliedOnDeathLevelAction {
    @Expose
    @Document(description = "The ID of the potion effect.")
    private String effect;

    @Expose
    @Document(description = "The amount of time to apply the effect for if specified as an entry or exit action; " +
            "continuous actions will ignore this.")
    private int duration;

    @Expose
    @OptionalField(defaultValue = "0")
    @Document(description = "The level to amplify the potion; the default, 0, will apply as a level 1 potion, 1 would apply " +
            "a level 2 potion, etc.")
    private int amplifier;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, the effect will be reapplied with the remaining duration on death; this only " +
            "applies when this is used as an entry or exit action, continuous effects will always reapply on death.")
    private boolean persistOnDeath;

    @Expose
    @SuppressWarnings({"FieldMayBeFinal"})
    @OptionalField(defaultValue = "True")
    @Document(description = "If true, potion effects from this action will stack with others of the same type from " +
            "other actions. This should generally be kept the same amongst all uses of the same effect, otherwise " +
            "things will become... interesting.")
    private boolean additive = true;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "Set if the potion effect should show particles or not.")
    private boolean particles;

    private Effect theEffect;

    @Override
    public void validate(final Need parentNeed, final NeedLevel parentLevel) throws IllegalArgumentException {
        if (effect == null || effect.isEmpty()) throw new IllegalArgumentException("Potion effect must be specified.");

        if (RegistryManager.ACTIVE.getRegistry(Effect.class).getValue(new ResourceLocation(effect)) == null) {
            throw new IllegalArgumentException("Unable to get potion effect '" + effect + "'.");
        }
    }

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
    public void onEntered(final Need need, final NeedLevel level, final PlayerEntity player) {
        final Effect eff = getTheEffect();
        if (eff == null) return;

        if (!additive) {
            player.addPotionEffect(new EffectInstance(eff, duration, amplifier, false, particles));
            return;
        }

        final EffectInstance old = player.getActivePotionEffect(eff);
        if (old == null) {
            player.addPotionEffect(new EffectInstance(eff, duration, amplifier, false, particles));
            return;
        }

        final EffectInstance inst = new EffectInstance(
                eff,
                duration == Integer.MAX_VALUE ? duration : (duration + old.getDuration()),
                (amplifier == 0 ? amplifier + 1 : amplifier) + old.getAmplifier(),
                false,
                particles);
        player.addPotionEffect(inst);
    }

    @Override
    public void onExited(final Need need, final NeedLevel level, final PlayerEntity player) {
        onEntered(need, level, player);
    }

    @Override
    public void onContinuousStart(final Need need, final NeedLevel level, final PlayerEntity player) {
        duration = Integer.MAX_VALUE;
        onEntered(need, level, player);
    }

    @Override
    public void onContinuousEnd(final Need need, final NeedLevel level, final PlayerEntity player) {
        final Effect eff = getTheEffect();
        if (eff == null) return;

        // Get the old instance:
        final EffectInstance inst = player.getActivePotionEffect(eff);
        if (inst == null) return;

        // Remove it from the player...
        player.removePotionEffect(eff);
        if (!additive || inst.getAmplifier() <= amplifier) return;

        // If we need to, let's reduce the amplifier...
        final int newAmp = inst.getAmplifier() - (amplifier == 0 ? 1 : amplifier);
        player.addPotionEffect(new EffectInstance(eff, inst.getDuration(), newAmp, false, particles));
    }

    @Override
    public void onRespawned(final Need need, final NeedLevel level, final PlayerEntity player, final PlayerEntity oldPlayer) {
        if (!persistOnDeath) return;

        final Effect eff = getTheEffect();
        if (eff == null) return;

        final EffectInstance effectInstance = oldPlayer.getActivePotionEffect(eff);
        if (effectInstance != null) player.addPotionEffect(effectInstance);
    }

    @Override
    public void onRespawnedWhenContinuous(final Need need, final NeedLevel level, final PlayerEntity player, final PlayerEntity oldPlayer) {
        onContinuousStart(need, level, player);
    }
}

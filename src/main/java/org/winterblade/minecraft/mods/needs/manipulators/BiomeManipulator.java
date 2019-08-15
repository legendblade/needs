package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
@Document(description = "Triggers while the player is in one of the specified biomes/biome types; at least one or the " +
        "other must be specified.")
public class BiomeManipulator extends BaseManipulator implements ICondition, ITrigger {
    @Expose
    @Document(description = "The amount to change by; optional when used as a condition or trigger")
    protected NeedExpressionContext amount;

    @Expose
    @OptionalField(defaultValue = "All")
    @Document(type = String.class, description = "A list of biomes that will trigger this")
    protected List<String> biomes = Collections.emptyList();

    @Expose
    @OptionalField(defaultValue = "All")
    @Document(type = String.class, description = "A list of biome types that will trigger this")
    protected List<String> biomeTypes = Collections.emptyList();

    protected List<BiomeManager.BiomeType> types = Collections.emptyList();
    protected final Map<Biome, Boolean> biomeMap = new HashMap<>();
    protected final Map<Biome, Boolean> biomeTypeMap = new HashMap<>();

    protected boolean trackTypes = false;
    protected boolean trackBiomes = false;
    private ConditionalManipulator parentCondition;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        validateCommon();
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        loadCommon();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::tickNeed);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        TickManager.INSTANCE.removePlayerTickUpdate(this);
        types = Collections.emptyList();
    }

    @Override
    public void validateCondition(final ConditionalManipulator parent) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onConditionLoaded(final ConditionalManipulator parent) {
        loadCommon();
    }

    @Override
    public void onConditionUnloaded() {
        types = Collections.emptyList();
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        final Biome biome = player.world.getBiome(new BlockPos(player));

        //noinspection ConstantConditions - I don't believe you.
        if (biome == null) return false;

        // This is a mess, but, simple
        return (trackBiomes && biomeMap.computeIfAbsent(biome, (kv) -> {
                final ResourceLocation key = RegistryManager.ACTIVE.getRegistry(Biome.class).getKey(biome);
                return key != null && biomes.contains(key.toString());
            }))
            ||
            (trackTypes && biomeTypeMap.computeIfAbsent(biome,
                    (kv) -> types.stream().anyMatch((t) -> {
                        final ImmutableList<BiomeManager.BiomeEntry> biomes = BiomeManager.getBiomes(t);
                        return biomes != null && biomes.stream().anyMatch((b) -> b.biome.equals(biome));
            })));
    }

    @Override
    public void validateTrigger(final ConditionalManipulator parent) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onTriggerLoaded(final ConditionalManipulator parent) {
        parentCondition = parent;
        loadCommon();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::tickTrigger);
    }

    @Override
    public void onTriggerUnloaded() {
        TickManager.INSTANCE.removePlayerTickUpdate(this);
        types = Collections.emptyList();
    }

    protected void validateCommon() {
        // Check if we have biomes; don't store them, as they may change between
        if (!biomeTypes.isEmpty()) {
            biomeTypes = biomeTypes.stream().filter((b) -> {
                try {
                    BiomeManager.BiomeType.valueOf(b.toUpperCase());
                } catch (final IllegalArgumentException e) {
                    throw new IllegalArgumentException("The biome type " + b + " isn't registered; valid values are: " + Arrays.toString(BiomeManager.BiomeType.values()));
                }
                return true;
            }).collect(Collectors.toList());
        }

        if (biomes.isEmpty() && biomeTypes.isEmpty()) throw new IllegalArgumentException("At least one valid biome or biome type must be specified.");
    }

    protected void loadCommon() {
        if (!biomeTypes.isEmpty()) {
            types = biomeTypes.stream().map((b) -> {
                try {
                    return BiomeManager.BiomeType.valueOf(b.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // This should be impossible by now, but, go off.
                    NeedsMod.LOGGER.info("The biome type " + b + " isn't registered.");
                    return null;
                }
            }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (!biomes.isEmpty()) trackBiomes = true;
        if (!types.isEmpty()) trackTypes = true;

        if (amount != null) amount.syncAll();
    }

    private void tickNeed(final PlayerEntity player) {
        if (!test(player)) return;
        parent.adjustValue(player, getAmount(player), this);
    }

    private void tickTrigger(final PlayerEntity player) {
        if (!test(player)) return;
        parentCondition.trigger(player, this);
    }
}

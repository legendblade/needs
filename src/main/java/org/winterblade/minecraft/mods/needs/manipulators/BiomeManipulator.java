package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class BiomeManipulator extends BaseManipulator {
    @Expose
    protected NeedExpressionContext amount;

    @Expose
    @Document(type = String.class)
    protected List<String> biomes = Collections.emptyList();

    @Expose
    @Document(type = String.class)
    protected List<String> biomeTypes = Collections.emptyList();

    protected List<BiomeManager.BiomeType> types = Collections.emptyList();
    protected final Map<Biome, Boolean> biomeMap = new HashMap<>();
    protected final Map<Biome, Boolean> biomeTypeMap = new HashMap<>();

    protected boolean trackTypes = false;
    protected boolean trackBiomes = false;

    @Override
    public void onCreated() {
        if (!biomeTypes.isEmpty()) {
            types = biomeTypes.stream().map((b) -> {
                try {
                    return BiomeManager.BiomeType.valueOf(b.toUpperCase());
                } catch (IllegalArgumentException e) {
                    NeedsMod.LOGGER.info("The biome type " + b + " isn't registered.");
                    return null;
                }
            }).filter(Objects::nonNull)
            .collect(Collectors.toList());
        }

        if (biomes.isEmpty() && types.isEmpty()) throw new JsonParseException("At least one valid biome or biome type must be specified.");

        if (!biomes.isEmpty()) trackBiomes = true;
        if (!types.isEmpty()) trackTypes = true;

        super.onCreated();
        TickManager.INSTANCE.requestPlayerTickUpdate(this::onTick);
    }

    private void onTick(final PlayerEntity player) {
        final Biome biome = player.world.getBiome(new BlockPos(player));

        //noinspection ConstantConditions - I don't believe you.
        if (biome == null) return;

        // This is a mess, but, simple
        if (
            (!trackBiomes || !biomeMap.computeIfAbsent(biome, (kv) -> {
                final ResourceLocation key = RegistryManager.ACTIVE.getRegistry(Biome.class).getKey(biome);
                return key != null && biomes.contains(key.toString());
            })) &&
            (!trackTypes || !biomeTypeMap.computeIfAbsent(biome,
                (kv) -> types.stream().anyMatch((t) -> {
                    final ImmutableList<BiomeManager.BiomeEntry> biomes = BiomeManager.getBiomes(t);
                    return biomes != null && biomes.stream().anyMatch((b) -> b.biome.equals(biome));
            })))
        ) return;

        amount.setCurrentNeedValue(parent, player);
        parent.adjustValue(player, amount.get(), this);
    }
}

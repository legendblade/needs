package org.winterblade.minecraft.mods.needs.capabilities.customneed;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.needs.CustomNeed;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess"})
@Mod.EventBusSubscriber
public class NeedCapability implements INeedCapability {
    protected Map<String, Double> values = new HashMap<>();
    protected Map<String, Map<String, Double>> levelAdjustments = new HashMap<>();
    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public double getValue(final String id) {
        return values.computeIfAbsent(id, k -> 0d);
    }

    @Override
    public void setValue(final String id, final double value) {
        values.put(id, value);
    }

    @Override
    public boolean isInitialized(final String id) {
        return values.containsKey(id);
    }

    @Nonnull
    @Override
    public Map<String, Double> getValues() {
        if (values == null) values = new HashMap<>();
        return values;
    }

    @Override
    public void storeLevelAdjustment(final String needName, final String levelName, final double adjustment) {
        levelAdjustments
            .computeIfAbsent(needName, (kv) -> new HashMap<>())
            .put(levelName, adjustment);
    }

    @Override
    public double getLevelAdjustment(final String needName, final String levelName) {
        return levelAdjustments
                .computeIfAbsent(needName, (kv) -> new HashMap<>())
                .computeIfAbsent(needName, (kv) -> 0d);
    }

    @Override
    public Map<String, Map<String, Double>> getLevelAdjustments() {
        return levelAdjustments;
    }

    @SubscribeEvent
    public static void attach(final AttachCapabilitiesEvent<Entity> evt) {
        if (!(evt.getObject() instanceof PlayerEntity)) return;

        evt.addCapability(new ResourceLocation("needs:custom_needs"), new Provider());
    }

    public static class Storage implements Capability.IStorage<INeedCapability> {
        public static final Storage INSTANCE = new Storage();

        private static final String CURRENT = "current";
        private static final String LEVELS = "levels";
        private static final String LAST_ADJUSTMENT = "lastAdjustment";

        @Nullable
        @Override
        public CompoundNBT writeNBT(final Capability<INeedCapability> capability, final INeedCapability instance, final Direction side) {
            final CompoundNBT nbt = new CompoundNBT();

            instance.getValues().forEach((needName, currentValue)-> {
                final CompoundNBT n = new CompoundNBT();
                n.putDouble(CURRENT, currentValue);
                nbt.put(needName, n);
            });

            final CompoundNBT levelAdjusts = new CompoundNBT();
            instance.getLevelAdjustments().forEach((needName, levelMap) -> {
                final CompoundNBT n = getOrDefaultNeed(nbt, needName);
                final CompoundNBT levels = new CompoundNBT();
                n.put(LEVELS, levels);

                // Currently don't write any levels whose adjustment is zero:
                levelMap.entrySet().stream().filter((kv) -> kv.getValue() != 0).collect(Collectors.toList()).forEach((kv) -> {
                    final CompoundNBT level = new CompoundNBT();
                    level.putDouble(LAST_ADJUSTMENT, kv.getValue());
                    levels.put(kv.getKey(), level);
                });
                nbt.put(needName, n);
            });

            return nbt;
        }

        @Override
        public void readNBT(final Capability<INeedCapability> capability, final INeedCapability instance, final Direction side, final INBT nbt) {
            if (!(nbt instanceof CompoundNBT)) {
                NeedsMod.LOGGER.error("Unable to deserialize custom need storage; nbt data is not a compound tag");
                return;
            }

            final CompoundNBT nbtCompound = (CompoundNBT) nbt;
            final Map<String, Double> currentLevels = instance.getValues();
            final Map<String, Map<String, Double>> levelAdjustments = instance.getLevelAdjustments();
            nbtCompound.keySet().forEach((k) -> {
                final CompoundNBT need = getOrDefaultNeed(nbtCompound, k);

                if (need.contains(CURRENT)) currentLevels.put(k, need.getDouble(CURRENT));

                if (!need.contains(LEVELS)) return;
                final CompoundNBT levels = need.getCompound(LEVELS);

                levels.keySet().forEach((level) -> {
                    final INBT levelNbt = levels.get(level);
                    if (!(levelNbt instanceof CompoundNBT)) return;

                    final CompoundNBT levelCompound = (CompoundNBT) levelNbt;
                    instance.storeLevelAdjustment(k, level, levelCompound.getDouble(LAST_ADJUSTMENT));
                });
            });
        }

        private CompoundNBT getOrDefaultNeed(final CompoundNBT root, final String need) {
            if (!root.contains(need)) {
                return new CompoundNBT();
            }

            final INBT nbt = root.get(need);
            if (nbt instanceof CompoundNBT) return (CompoundNBT) nbt;
            if (!(nbt instanceof DoubleNBT)) return new CompoundNBT();

            return new CompoundNBT();
        }
    }

    static class Provider extends CapabilityProvider<Provider> implements INBTSerializable<CompoundNBT> {
        private final INeedCapability theActualBloodyCap = new NeedCapability();
        private final LazyOptional<INeedCapability> capability = LazyOptional.of(() -> theActualBloodyCap);

        protected Provider() {
            super(Provider.class);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
            return cap == CustomNeed.CAPABILITY ? capability.cast() : super.getCapability(cap, side);
        }

        @Override
        public CompoundNBT serializeNBT() {
            return Storage.INSTANCE.writeNBT(CustomNeed.CAPABILITY, theActualBloodyCap, null);
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt) {
            Storage.INSTANCE.readNBT(CustomNeed.CAPABILITY, theActualBloodyCap, null, nbt);
        }
    }
}

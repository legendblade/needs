package org.winterblade.minecraft.mods.needs.capabilities.latch;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.IItemUsedCountCapability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class LatchedCapability implements ILatchedCapability {
    static {
        CAPABILITY = null;
    }

    @CapabilityInject(ILatchedCapability.class)
    private final Map<String, Boolean> values = new HashMap<>();

    @Override
    public boolean lastValue(final String key) {
        return values.getOrDefault(key, false);
    }

    @Override
    public void setValue(final String key, final boolean value) {
        values.put(key, value);
    }

    @Nonnull
    @Override
    public Map<String, Boolean> getValues() {
        return values;
    }

    @SuppressWarnings("WeakerAccess")
    @CapabilityInject(IItemUsedCountCapability.class)
    public static Capability<ILatchedCapability> CAPABILITY;

    public static class Storage implements Capability.IStorage<ILatchedCapability> {
        public static final Storage INSTANCE = new Storage();

        @Nullable
        @Override
        public CompoundNBT writeNBT(final Capability<ILatchedCapability> capability, final ILatchedCapability instance, final Direction side) {
            final CompoundNBT out = new CompoundNBT();
            instance.getValues().forEach(out::putBoolean);
            return out;
        }

        @Override
        public void readNBT(final Capability<ILatchedCapability> capability, final ILatchedCapability instance, final Direction side, final INBT nbt) {
            if (!(nbt instanceof CompoundNBT)) {
                NeedsMod.LOGGER.error("Unable to deserialize item use storage; NBT data is not a compound tag");
                return;
            }

            final CompoundNBT holder = (CompoundNBT) nbt;
            holder.keySet().forEach((k) -> instance.setValue(k, holder.getBoolean(k)));
        }
    }

    public static class Provider extends CapabilityProvider<Provider> implements INBTSerializable<CompoundNBT> {
        private final ILatchedCapability instance = new LatchedCapability();
        private final LazyOptional<ILatchedCapability> capability = LazyOptional.of(() -> instance);

        public Provider() {
            super(Provider.class);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
            return cap == LatchedCapability.CAPABILITY ? capability.cast() : super.getCapability(cap, side);
        }

        @Override
        public CompoundNBT serializeNBT() {
            return Storage.INSTANCE.writeNBT(LatchedCapability.CAPABILITY, instance, null);
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt) {
            Storage.INSTANCE.readNBT(LatchedCapability.CAPABILITY, instance, null, nbt);
        }
    }
}

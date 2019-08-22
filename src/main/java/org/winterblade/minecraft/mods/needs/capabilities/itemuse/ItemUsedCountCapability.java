package org.winterblade.minecraft.mods.needs.capabilities.itemuse;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.manipulators.ItemUsedCountManipulator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemUsedCountCapability implements IItemUsedCountCapability {
    private final Map<String, Map<String, ItemUseStorage>> storage = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, Map<String, ItemUseStorage>> getStorage() {
        return storage;
    }

    @Override
    public Map<String, ItemUseStorage> getStorage(final String key) {
        return storage.computeIfAbsent(key, (k) -> new HashMap<>());
    }

    public static class Storage implements Capability.IStorage<IItemUsedCountCapability> {
        public static final Storage INSTANCE = new Storage();

        @Nullable
        @Override
        public CompoundNBT writeNBT(final Capability<IItemUsedCountCapability> capability, final IItemUsedCountCapability instance, final Direction side) {
            final CompoundNBT out = new CompoundNBT();

            instance
                .getStorage()
                .forEach((k, v) -> {
                    if (v.isEmpty()) return;
                    out.put(k,
                        v
                            .values()
                            .stream()
                            .map(ItemUseStorage::serializeNBT)
                            .collect(Collectors.toCollection(ListNBT::new))
                    );
                });

            return out;
        }

        @Override
        public void readNBT(final Capability<IItemUsedCountCapability> capability, final IItemUsedCountCapability instance, final Direction side, final INBT nbt) {
            if (!(nbt instanceof CompoundNBT)) {
                NeedsMod.LOGGER.error("Unable to deserialize item use storage; NBT data is not a compound tag");
                return;
            }

            final CompoundNBT holder = (CompoundNBT) nbt;
            final Map<String, Map<String, ItemUseStorage>> storage = instance.getStorage();
            holder.keySet().forEach((k) -> {
                final ListNBT list = holder.getList(k, 10); // Magic number? Isn't there an enum for this somewhere?
                storage.put(k,
                    list
                        .stream()
                        .map((v) -> {
                            final ItemUseStorage item = new ItemUseStorage();
                            item.deserializeNBT((CompoundNBT)v);
                            return item;
                        })
                        .collect(Collectors.toMap(ItemUseStorage::getKey, (i) -> i))
                );
            });
        }
    }

    public static class Provider extends CapabilityProvider<Provider> implements INBTSerializable<CompoundNBT> {
        private final IItemUsedCountCapability instance = new ItemUsedCountCapability();
        private final LazyOptional<IItemUsedCountCapability> capability = LazyOptional.of(() -> instance);

        public Provider() {
            super(Provider.class);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
            return cap == ItemUsedCountManipulator.CAPABILITY ? capability.cast() : super.getCapability(cap, side);
        }

        @Override
        public CompoundNBT serializeNBT() {
            return Storage.INSTANCE.writeNBT(ItemUsedCountManipulator.CAPABILITY, instance, null);
        }

        @Override
        public void deserializeNBT(final CompoundNBT nbt) {
            Storage.INSTANCE.readNBT(ItemUsedCountManipulator.CAPABILITY, instance, null, nbt);
        }
    }
}

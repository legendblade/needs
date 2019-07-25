package org.winterblade.minecraft.mods.needs.needs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
class CustomNeedCapability implements ICustomNeedCapability {
    protected Map<String, Double> values = new HashMap<>();

    @Override
    public double getValue(String id) {
        return values.computeIfAbsent(id, k -> 0d);
    }

    @Override
    public void setValue(String id, double value) {
        values.put(id, value);
    }

    @Override
    public boolean isInitialized(String id) {
        return values.containsKey(id);
    }

    @Override
    public Map<String, Double> getValues() {
        if (values == null) values = new HashMap<>();
        return values;
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> evt) {
        if (!(evt.getObject() instanceof PlayerEntity)) return;

        evt.addCapability(new ResourceLocation("needs:custom_needs"), new Provider());
    }

    static class Storage implements Capability.IStorage<ICustomNeedCapability> {
        static final Storage INSTANCE = new Storage();

        @Nullable
        @Override
        public CompoundNBT writeNBT(Capability<ICustomNeedCapability> capability, ICustomNeedCapability instance, Direction side) {
            CompoundNBT nbt = new CompoundNBT();
            instance.getValues().forEach(nbt::putDouble);
            return nbt;
        }

        @Override
        public void readNBT(Capability<ICustomNeedCapability> capability, ICustomNeedCapability instance, Direction side, INBT nbt) {
            if (!(nbt instanceof CompoundNBT)) {
                NeedsMod.LOGGER.error("Unable to deserialize custom need storage; nbt data is not a compound tag");
                return;
            }

            CompoundNBT nbtCompound = (CompoundNBT) nbt;
            nbtCompound.keySet().forEach((k) -> instance.getValues().put(k, nbtCompound.getDouble(k)));
        }
    }

    static class Provider implements ICapabilityProvider, INBTSerializable<CompoundNBT> {
        private final ICustomNeedCapability theActualBloodyCap = new CustomNeedCapability();
        private final LazyOptional<ICustomNeedCapability> capability = LazyOptional.of(() -> theActualBloodyCap);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            //noinspection ConstantConditions
            return cap != CustomNeed.CAPABILITY ? null : capability.cast();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return Storage.INSTANCE.writeNBT(CustomNeed.CAPABILITY, theActualBloodyCap, null);
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            Storage.INSTANCE.readNBT(CustomNeed.CAPABILITY, theActualBloodyCap, null, nbt);
        }
    }
}

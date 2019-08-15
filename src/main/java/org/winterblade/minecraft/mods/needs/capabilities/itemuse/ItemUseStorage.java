package org.winterblade.minecraft.mods.needs.capabilities.itemuse;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

public class ItemUseStorage implements Comparable<ItemUseStorage>, INBTSerializable<CompoundNBT> {
    private String key;
    private long lastBenefitTick;
    private int count = 0;

    public static String getKeyFrom(final ItemStack item, final boolean trackMeta) {
        String key = item.getItem().getRegistryName().toString();
        if (trackMeta) key += ":" + item.getDamage();
        return key;
    }

    @Override
    public int compareTo(final ItemUseStorage o) {
        if (o == null) return -1;
        return Long.compare(this.lastBenefitTick, o.lastBenefitTick);
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();

        nbt.putString("key", key);
        nbt.putLong("lastBenefitTick", lastBenefitTick);
        nbt.putInt("count", count);

        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        key = nbt.getString("key");
        lastBenefitTick = nbt.getLong("lastBenefitTick");
        count = nbt.getInt("count");
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        count++;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public long getLastBenefitTick() {
        return lastBenefitTick;
    }

    public void setLastBenefitTick(final long lastBenefitTick) {
        this.lastBenefitTick = lastBenefitTick;
    }
}

package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import org.apache.commons.lang3.StringUtils;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.needs.Need;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
@Document(description = "A version of Item Used that limits the number of times the player can benefit from the effect per item")
public class ItemUsedCountManipulator extends ItemUsedManipulator {
    static {
        CAPABILITY = null;
    }

    @CapabilityInject(IItemUsedCountCapability.class)
    protected static final Capability<IItemUsedCountCapability> CAPABILITY;

    @Expose
    @Document(description = "The default amount to apply if not specified on an item level")
    private CountedFoodExpressionContext defaultAmount;

    @Expose
    @Document(description = "An key/value map of items/amounts, or array of items using the default amount")
    @JsonAdapter(FoodItemValueDeserializer.class)
    protected final Map<Predicate<ItemStack>, ExpressionContext> items = new HashMap<>();

    @Expose
    @Document(description = "A unique key that will be used to store data about item usage; only letters, underscores, and numbers will be recognized")
    protected String id;

    @Expose
    @Document(description = "The number of times the player can benefit from the use of the item; if this is an expression, it will be rounded down " +
            "to the nearest whole number.")
    @OptionalField(defaultValue = "1")
    protected NeedExpressionContext uses = ExpressionContext.makeConstant(new NeedExpressionContext(), 1);

    @Expose
    @Document(description = "The number of items to remember. Items will fall off the list in order of the least recently " +
            "benefited from; e.g. if uses is 2, and numberStored is also 2, then using Item A, then item B, then Item A, if you were " +
            "to use Item C, then item B would fall off the list, because Item A was benefited from more recently. If this is an " +
            "expression, the return value will be rounded down to the nearest whole number (e.g. 0.9 -> 0); if this is less than the size " +
            "of the list at the time the list is being checked, items will automatically be dropped until the list is down to the proper size.")
    @OptionalField(defaultValue = "No limit(ish)")
    protected NeedExpressionContext numberStored = ExpressionContext.makeConstant(new NeedExpressionContext(), Integer.MAX_VALUE);

    @Expose
    @Document(description = "Should damage values be treated as separate items?")
    @OptionalField(defaultValue = "False")
    protected boolean trackDamage = false;

    private String storageKey;
    private boolean checkNumberStored;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (uses.isConstant() && uses.get() <= 0) throw new IllegalArgumentException("Uses must be a positive whole number if it's constant.");
        if (numberStored.isConstant() && numberStored.get() <= 0) throw new IllegalArgumentException("Number stored must be a positive whole number if it's constant.");
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID must be provided.");
        checkNumberStored = numberStored.isConstant() && numberStored.get() < Integer.MAX_VALUE;
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        storageKey = "itemusedcount_" + StringUtils.lowerCase(id).replaceAll("[^a-z 0-9]", "").replaceAll(" ", "_");
    }

    @Override
    public void onItemUsed(final LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final WeakReference<PlayerEntity> playerRef = new WeakReference<>((PlayerEntity) evt.getEntityLiving());

        // This is a bit heavier; we want to put it off until after the player is done with the item.
        TickManager.INSTANCE.doLater(() -> {
            final PlayerEntity player = playerRef.get();
            if (player == null) return; // If the player has gone away since we queued this, exit early.

            // Make sure the item itself matches:
            final ItemStack item = evt.getItem();
            final ExpressionContext expr = checkItem(item);
            if (expr == null) return;

            // Get a bunch of stuff that we're going to need to unravel this mess
            final String key = ItemUseStorage.getKeyFrom(item, trackDamage);
            final IItemUsedCountCapability cap = player.getCapability(CAPABILITY).orElse(new ItemUsedCountCapability());
            final Map<String, ItemUseStorage> storage = cap.getStorage(storageKey);

            // Check if we had one, otherwise create it
            ItemUseStorage itemStore = storage.get(key);
            final boolean existed = itemStore != null;
            if (!existed) {
                itemStore = new ItemUseStorage();
                itemStore.setKey(key);
            }

            // Get the max number
            uses.setCurrentNeedValue(parent, player);
            double maxUse = Math.floor(uses.get());
            if (maxUse <= 0) return;

            if (!checkNumberStored) {
                // If we're over the limit:
                if (maxUse <= itemStore.getCount()) return;

                // Let's get out of here.
                if (!existed) storage.put(key, itemStore);
                prepHandle(player, item, expr, itemStore);
                return;
            }

            // Now get into the larger pain...
            numberStored.setCurrentNeedValue(parent, player);
            double count = numberStored.get();

            // If it's an expression that uses need, it's possible, and easier...
            if (count < 1) {
                if (!storage.isEmpty()) storage.clear();
                if (maxUse <= itemStore.getCount()) return;
                prepHandle(player, item, expr, itemStore);
                return;
            }

            // If the storage is empty, that means the item didn't exist
            // and we have at least one item of space.
            if (storage.isEmpty()) {
                if (maxUse <= itemStore.getCount()) return;
                storage.put(key, itemStore);
                prepHandle(player, item, expr, itemStore);
                return;
            }

            // If we're already in the list, don't worry about it.
            if (existed && storage.size() < count) {
                if (maxUse <= itemStore.getCount()) return;
                prepHandle(player, item, expr, itemStore);
                return;
            }

            // If we still have room for one more
            if (!existed && storage.size() < count - 1) {
                if (maxUse <= itemStore.getCount()) return;
                storage.put(key, itemStore);
                prepHandle(player, item, expr, itemStore);
                return;
            }

            // Build up our stream...
            Stream<ItemUseStorage> stream = storage
                .values()
                .stream()
                .sorted(Comparator.comparing(ItemUseStorage::getLastBenefitTick).reversed())
                .limit((long) Math.floor(count));

            // This will pop it to the front
            itemStore.setLastBenefitTick(player.world.getGameTime());

            final AtomicBoolean inReducedSet = new AtomicBoolean();
            if (existed) {
                ItemUseStorage dawnOfTheFinalStore = itemStore;
                stream = stream.peek((i) -> inReducedSet.set(i.equals(dawnOfTheFinalStore)));
            } else {
                inReducedSet.set(false);
            }

            LinkedList<ItemUseStorage> reducedList = stream.collect(Collectors.toCollection(LinkedList::new));

            if (!inReducedSet.get()) {
                // If we're going to add it, we need to trim off the oldest element
                reducedList.pollLast();
                reducedList.addFirst(itemStore);

                // Reset its count, and handle it.
                itemStore.setCount(0);
                handle(player, item, expr);
                itemStore.incrementCount();
            } else if (itemStore.getCount() < maxUse) {
                // Otherwise, if we still have a use, use it here
                handle(player, item, expr);
                itemStore.incrementCount();
            }

            // Reduce the list...
            storage.clear();
            reducedList.forEach((v) -> storage.put(v.getKey(), v));
        });
    }

    /**
     * Preps and handles the expressions
     * @param player    The player
     * @param item      The item
     * @param expr      The expression
     * @param itemStore The item store to update
     */
    protected void prepHandle(PlayerEntity player, ItemStack item, ExpressionContext expr, ItemUseStorage itemStore) {
        itemStore.setLastBenefitTick(player.world.getGameTime());
        handle(player, item, expr);
        itemStore.incrementCount();
    }

    @Override
    protected void setupExpression(Supplier<Double> currentValue, PlayerEntity player, ItemStack item, ExpressionContext expr) {

        super.setupExpression(currentValue, player, item, expr);
    }

    @Override
    public FoodExpressionContext getDefaultAmount() {
        return defaultAmount;
    }

    @Override
    public Map<Predicate<ItemStack>, ExpressionContext> getItems() {
        return items;
    }

    protected static class FoodItemValueDeserializer extends ItemValueDeserializer {
        @Override
        public Map<Predicate<ItemStack>, ExpressionContext> deserialize(final JsonElement itemsEl, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return super.deserialize(itemsEl, context, CountedFoodExpressionContext.class);
        }
    }

    @JsonAdapter(ExpressionContext.Deserializer.class)
    public static class CountedFoodExpressionContext extends FoodExpressionContext {
        public static final String COUNT = "count";

        protected static final Map<String, String> docs = new HashMap<>(FoodExpressionContext.docs);
        static {
            docs.put(COUNT, "The number of times this item has been used prior.");
        }

        public CountedFoodExpressionContext() {
        }

        @Override
        public List<String> getElements() {
            final List<String> elements = super.getElements();
            elements.add(COUNT);
            return elements;
        }

        @Override
        public Map<String, String> getElementDocumentation() {
            return docs;
        }
    }

    protected static class ItemUseStorage implements Comparable<ItemUseStorage>, INBTSerializable<CompoundNBT> {
        private String key;
        private long lastBenefitTick;
        private int count = 0;

        public static String getKeyFrom(ItemStack item, boolean trackMeta) {
            String key = item.getItem().getRegistryName().toString();
            if (trackMeta) key += ":" + item.getDamage();
            return key;
        }

        @Override
        public int compareTo(ItemUseStorage o) {
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
        public void deserializeNBT(CompoundNBT nbt) {
            key = nbt.getString("key");
            lastBenefitTick = nbt.getLong("lastBenefitTick");
            count = nbt.getInt("count");
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
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

        public void setLastBenefitTick(long lastBenefitTick) {
            this.lastBenefitTick = lastBenefitTick;
        }
    }

    public interface IItemUsedCountCapability {
        Map<String, Map<String, ItemUseStorage>> getStorage();

        Map<String, ItemUseStorage> getStorage(String key);
    }

    public static class ItemUsedCountCapability implements IItemUsedCountCapability {
        private final Map<String, Map<String, ItemUseStorage>> storage = new HashMap<>();

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
                CompoundNBT out = new CompoundNBT();

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
                    ListNBT list = holder.getList(k, 10); // Magic number? Isn't there an enum for this somewhere?
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

        protected static class Provider extends CapabilityProvider<Provider> implements INBTSerializable<CompoundNBT> {
            private final IItemUsedCountCapability instance = new ItemUsedCountCapability();
            private final LazyOptional<IItemUsedCountCapability> capability = LazyOptional.of(() -> instance);

            protected Provider() {
                super(Provider.class);
            }

            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
                return cap == CAPABILITY ? capability.cast() : super.getCapability(cap, side);
            }

            @Override
            public CompoundNBT serializeNBT() {
                return Storage.INSTANCE.writeNBT(CAPABILITY, instance, null);
            }

            @Override
            public void deserializeNBT(final CompoundNBT nbt) {
                Storage.INSTANCE.readNBT(CAPABILITY, instance, null, nbt);
            }
        }
    }
}

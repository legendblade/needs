package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import org.apache.commons.lang3.StringUtils;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.NeedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.IItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.ItemUseStorage;
import org.winterblade.minecraft.mods.needs.capabilities.itemuse.ItemUsedCountCapability;
import org.winterblade.minecraft.mods.needs.expressions.CountedFoodExpressionContext;
import org.winterblade.minecraft.mods.needs.expressions.FoodExpressionContext;
import org.winterblade.minecraft.mods.needs.util.items.FoodItemValueDeserializer;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Document(description = "A version of Item Used that limits the number of times the player can benefit from the effect per item")
public class ItemUsedCountManipulator extends ItemUsedManipulator {
    static {
        CAPABILITY = null;
    }

    @CapabilityInject(IItemUsedCountCapability.class)
    public static Capability<IItemUsedCountCapability> CAPABILITY;

    @Expose
    @Document(description = "The default amount to apply if not specified on an item level")
    private CountedFoodExpressionContext defaultAmount;

    @Expose
    @Document(description = "An key/value map of items/amounts, or array of items using the default amount")
    @JsonAdapter(FoodItemValueDeserializer.class)
    protected final Map<Predicate<ItemStack>, ExpressionContext> items = new HashMap<>();

    @Expose
    @Document(description = "A key that will be used to store data about item usage; only letters, underscores, and " +
            "numbers will be recognized. Generally keys should be unique unless you intend to share use counts across " +
            "multiple instances.")
    protected String id;

    @Expose
    @Document(description = "The number of times the player can benefit from the use of the item; if this is an expression, it will be rounded down " +
            "to the nearest whole number.")
    @OptionalField(defaultValue = "1")
    protected NeedExpressionContext uses = ExpressionContext.makeConstant(new NeedExpressionContext(), 1);

    @Expose
    @Document(description = "The number of items to remember.\n\n" +
            "Items will fall off the list in order of the least recently " +
            "benefited from; e.g. if uses is 2, and numberStored is also 2, then using Item A, then item B, then Item A, if you were " +
            "to use Item C, then item B would fall off the list, because Item A was benefited from more recently.\n\n" +
            "If this is an " +
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
        validateUsesCommon();
        super.validate(need);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        onLoadUsesCommon();
    }

    @Override
    public void validateTrigger(final ConditionalManipulator parent) throws IllegalArgumentException {
        super.validateTrigger(parent);
        validateUsesCommon();
    }

    @Override
    public void onTriggerLoaded(final ConditionalManipulator parent) {
        super.onTriggerLoaded(parent);
        onLoadUsesCommon();
    }

    @Override
    public void asCommon(final LivingEntityUseItemEvent.Finish evt, final Consumer<PlayerEntity> callback) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;
        final WeakReference<PlayerEntity> playerRef = new WeakReference<>((PlayerEntity) evt.getEntityLiving());

        // This is a bit heavier; we want to put it off until after the player is done with the item.
        TickManager.INSTANCE.doLater(() -> {
            final PlayerEntity player = playerRef.get();
            if (player == null) return; // If the player has gone away since we queued this, exit early.

            // Make sure the item itself matches:
            lastItem = evt.getItem();
            lastMatch = checkItem(lastItem);
            if (lastMatch == null) return;

            // Get a bunch of stuff that we're going to need to unravel this mess
            final String key = ItemUseStorage.getKeyFrom(lastItem, trackDamage);
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
            final double maxUse = Math.floor(uses.apply(player));
            if (maxUse <= 0) return;

            if (!checkNumberStored) {
                // If we're over the limit:
                if (maxUse <= itemStore.getCount()) return;

                // Let's get out of here.
                if (!existed) storage.put(key, itemStore);
                prepHandle(player, itemStore, callback);
                return;
            }

            // Now get into the larger pain...
            numberStored.setCurrentNeedValue(parent, player);
            final double count = numberStored.apply(player);

            // If it's an expression that uses need, it's possible, and easier...
            if (count < 1) {
                if (!storage.isEmpty()) storage.clear();
                if (maxUse <= itemStore.getCount()) return;
                prepHandle(player, itemStore, callback);
                return;
            }

            // If the storage is empty, that means the item didn't exist
            // and we have at least one item of space.
            if (storage.isEmpty()) {
                if (maxUse <= itemStore.getCount()) return;
                storage.put(key, itemStore);
                prepHandle(player, itemStore, callback);
                return;
            }

            // If we're already in the list, don't worry about it.
            if (existed && storage.size() < count) {
                if (maxUse <= itemStore.getCount()) return;
                prepHandle(player, itemStore, callback);
                return;
            }

            // If we still have room for one more
            if (!existed && storage.size() < count - 1) {
                if (maxUse <= itemStore.getCount()) return;
                storage.put(key, itemStore);
                prepHandle(player, itemStore, callback);
                return;
            }

            // This will pop it to the front
            itemStore.setLastBenefitTick(player.world.getGameTime());

            // Build up our stream...
            Stream<ItemUseStorage> stream = storage
                .values()
                .stream()
                .sorted(Comparator.comparing(ItemUseStorage::getLastBenefitTick).reversed())
                .limit((long) Math.floor(count));

            final AtomicBoolean inReducedSet = new AtomicBoolean();
            if (existed) {
                final ItemUseStorage dawnOfTheFinalStore = itemStore;
                stream = stream.peek((i) -> inReducedSet.set(i.equals(dawnOfTheFinalStore)));
            } else {
                inReducedSet.set(false);
            }

            final LinkedList<ItemUseStorage> reducedList = stream.collect(Collectors.toCollection(LinkedList::new));

            if (!inReducedSet.get()) {
                // If we're going to add it, we need to trim off the oldest element
                reducedList.pollLast();
                reducedList.addFirst(itemStore);

                // Reset its count, and handle it.
                itemStore.setCount(0);
                prepHandle(player, itemStore, callback);
            } else if (itemStore.getCount() < maxUse) {
                // Otherwise, if we still have a use, use it here
                prepHandle(player, itemStore, callback);
            }

            // Reduce the list...
            storage.clear();
            reducedList.forEach((v) -> storage.put(v.getKey(), v));
        });
    }

    @Override
    protected void setupExpression(final Supplier<Double> currentValue, final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        super.setupExpression(currentValue, player, item, expr);
        if (!expr.isRequired(CountedFoodExpressionContext.COUNT)) return;

        final String key = ItemUseStorage.getKeyFrom(lastItem, trackDamage);
        final IItemUsedCountCapability cap = player.getCapability(CAPABILITY).orElse(new ItemUsedCountCapability());
        final Map<String, ItemUseStorage> storage = cap.getStorage(storageKey);

        final ItemUseStorage itemStore = storage.get(key);
        expr.setIfRequired(CountedFoodExpressionContext.COUNT, itemStore != null ? (() -> (double) itemStore.getCount()) : (() -> 0d));
    }

    private void validateUsesCommon() {
        if (uses.isConstant() && uses.apply(null) <= 0) throw new IllegalArgumentException("Uses must be a positive whole number if it's constant.");
        if (numberStored.isConstant() && numberStored.apply(null) <= 0) throw new IllegalArgumentException("Number stored must be a positive whole number if it's constant.");
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID must be provided.");
    }

    private void onLoadUsesCommon() {
        storageKey = "itemusedcount_" + StringUtils.lowerCase(id).replaceAll("[^a-z 0-9]", "").replaceAll(" ", "_");
        checkNumberStored = numberStored.isConstant() && numberStored.apply(null) < Integer.MAX_VALUE;
    }

    @Override
    public FoodExpressionContext getDefaultAmount() {
        return defaultAmount;
    }

    @Override
    public Map<Predicate<ItemStack>, ExpressionContext> getItems() {
        return items;
    }

    /**
     * Preps and handles the expressions
     * @param player    The player
     * @param itemStore The item store to update
     */
    private static void prepHandle(final PlayerEntity player, final ItemUseStorage itemStore, final Consumer<PlayerEntity> callback) {
        itemStore.setLastBenefitTick(player.world.getGameTime());
        callback.accept(player);
        itemStore.incrementCount();
    }

}

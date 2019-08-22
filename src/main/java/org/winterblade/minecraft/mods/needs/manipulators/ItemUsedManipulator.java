package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ITriggerable;
import org.winterblade.minecraft.mods.needs.api.manipulators.TooltipManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.expressions.FoodExpressionContext;
import org.winterblade.minecraft.mods.needs.util.items.ItemValueDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@Document(description = "Triggered when the player uses an item in the list")
public class ItemUsedManipulator extends TooltipManipulator implements ITrigger {
    @Expose
    @Document(description = "The default amount to apply if not specified on an item level")
    private FoodExpressionContext defaultAmount;

    @Expose
    @Document(description = "An key/value map of items/amounts, or array of items using the default amount")
    @JsonAdapter(ItemValueDeserializer.class)
    protected final Map<Predicate<ItemStack>, ExpressionContext> items = new HashMap<>();

    protected ExpressionContext lastMatch;
    protected ItemStack lastItem;
    protected ITriggerable parentCondition;

    public ItemUsedManipulator() {
        postFormat = (sb, player) -> sb.append("  (Use)").toString();
    }

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        validateCommon();
        if (getDefaultAmount() == null && getItems().entrySet().stream().anyMatch((kv) -> kv.getValue() == null)) {
            throw new IllegalArgumentException("Default amount must be specified if one or more items use it.");
        }

        super.validate(need);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        onLoadedCommon();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asManipulator);
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ITriggerable parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ITriggerable parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        onLoadedCommon();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::asTrigger);
    }

    @Override
    public void onTriggerUnloaded() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (lastMatch == null) return 0;

        setupExpression(() -> parent.getValue(player), player, lastItem, lastMatch);
        return lastMatch.apply(player);
    }

    protected void validateCommon() {
        //noinspection ConstantConditions - never, ever trust user input.
        if (getItems() == null || getItems().isEmpty()) throw new IllegalArgumentException("Items array must be specified.");
    }

    protected void onLoadedCommon() {
        getItems().entrySet().forEach((kv) -> {
            if (kv.getValue() == null) kv.setValue(getDefaultAmount());
            kv.getValue().build();
        });
    }

    /**
     * Handles checking the item and adjusting the need for the player
     * @param item   The item to check
     */
    protected ExpressionContext checkItem(final ItemStack item) {
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : getItems().entrySet()) {
            if (!entry.getKey().test(item)) continue;
            return entry.getValue();
        }
        return null;
    }

    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        for (final Map.Entry<Predicate<ItemStack>, ExpressionContext> entry : getItems().entrySet()) {
            if (entry.getKey().test(item)) return entry.getValue();
        }
        return null;
    }

    /**
     * Sets up the expression
     * @param player The player
     * @param item   The item
     * @param expr   The expression
     */
    protected void setupExpression(final Supplier<Double> currentValue, final PlayerEntity player, final ItemStack item, final ExpressionContext expr) {
        super.setupExpression(currentValue, player, item, expr);

        if (item.isFood()) {
            final Food food = item.getItem().getFood();
            if (food != null) {
                expr.setIfRequired(FoodExpressionContext.HUNGER, () -> (double) food.getHealing());
                expr.setIfRequired(FoodExpressionContext.SATURATION, () -> (double) food.getSaturation());
            } else {
                expr.setIfRequired(FoodExpressionContext.HUNGER, () -> 0d);
                expr.setIfRequired(FoodExpressionContext.SATURATION, () -> 0d);
            }
        } else {
            expr.setIfRequired(FoodExpressionContext.HUNGER, () -> 0d);
            expr.setIfRequired(FoodExpressionContext.SATURATION, () -> 0d);
        }
    }

    public void asManipulator(final LivingEntityUseItemEvent.Finish evt) {
        lastMatch = null;
        lastItem = null;

        asCommon(evt, this::handleManipulator);
    }

    public void asTrigger(final LivingEntityUseItemEvent.Finish evt) {
        lastMatch = null;
        lastItem = null;

        asCommon(evt, this::handleTrigger);
    }

    public void asCommon(final LivingEntityUseItemEvent.Finish evt, final Consumer<PlayerEntity> callback) {
        if (evt.getEntity().world.isRemote || !(evt.getEntityLiving() instanceof PlayerEntity)) return;

        TickManager.INSTANCE.doLater(() -> {
            final PlayerEntity player = (PlayerEntity) evt.getEntityLiving();

            lastItem = evt.getItem();
            lastMatch = checkItem(lastItem);

            if (lastMatch == null) return;
            callback.accept(player);
        });
    }

    /**
     * Handles actually setting the value for the player
     * @param player The player
     */
    protected void handleManipulator(final PlayerEntity player) {
        parent.adjustValue(player, getAmount(player), this);
    }

    /**
     * Handles actually setting the value for the player
     * @param player The player
     */
    protected void handleTrigger(final PlayerEntity player) {
        parentCondition.trigger(player, this);
    }

    public FoodExpressionContext getDefaultAmount() {
        return defaultAmount;
    }

    public Map<Predicate<ItemStack>, ExpressionContext> getItems() {
        return items;
    }

}

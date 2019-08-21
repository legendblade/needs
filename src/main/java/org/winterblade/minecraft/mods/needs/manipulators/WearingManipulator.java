package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import org.winterblade.minecraft.mods.needs.api.ICondition;
import org.winterblade.minecraft.mods.needs.api.ITrigger;
import org.winterblade.minecraft.mods.needs.api.OptionalField;
import org.winterblade.minecraft.mods.needs.api.TickManager;
import org.winterblade.minecraft.mods.needs.api.documentation.Document;
import org.winterblade.minecraft.mods.needs.api.expressions.CountedExpressionContext;
import org.winterblade.minecraft.mods.needs.api.expressions.ExpressionContext;
import org.winterblade.minecraft.mods.needs.api.manipulators.ConditionalManipulator;
import org.winterblade.minecraft.mods.needs.api.manipulators.TooltipManipulator;
import org.winterblade.minecraft.mods.needs.api.needs.Need;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Document(description = "Triggered every 5 ticks while the player is wearing the specific item(s)")
public class WearingManipulator extends TooltipManipulator implements ICondition, ITrigger {
    @Expose
    @Document(description = "The amount to change; count refers to the number of matching item(s)")
    protected CountedExpressionContext amount;

    @Expose
    @OptionalField(defaultValue = "True")
    @Document(description = "If true, check the player's chest slot for the item.")
    protected boolean chest = true;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, check the player's head slot for the item.")
    protected boolean head = false;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, check the player's legs slot for the item.")
    protected boolean legs = false;

    @Expose
    @OptionalField(defaultValue = "False")
    @Document(description = "If true, check the player's feet slot for the item.")
    protected boolean feet = false;

    // TODO: Curios

    @Expose
    @JsonAdapter(IIngredient.ToListDeserializer.class)
    @Document(type = IIngredient.class, description = "An array of items to check against, specified either as a single " +
            "item (e.g. `'minecraft:cobblestone'`) or a tag (e.g. `'tag:minecraft:leaves'`)")
    protected List<IIngredient> items;

    private long lastCount;
    private ConditionalManipulator parentCondition;
    private List<ItemStack> wornItems;

    @Override
    public void validate(final Need need) throws IllegalArgumentException {
        if (amount == null) throw new IllegalArgumentException("Amount must be specified.");
        validateCommon();
        super.validate(need);
    }


    @Override
    public void onLoaded() {
        amount.build();
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asManipulator);
        super.onLoaded();

        final String amountType = amount.isConstant() || !amount.isRequired(CountedExpressionContext.COUNT) ? "" : ", Per Item";
        final String suffix;
        final List<String> slots = new ArrayList<>();
        if (head) slots.add("Head");
        if (chest) slots.add("Chest");
        if (legs) slots.add("Legs");
        if (feet) slots.add("Feet");
        wornItems = new ArrayList<>(slots.size());

        suffix = "  (Worn on " + slots.stream().collect(Collectors.joining("/")) + amountType + ")";
        postFormat = (sb, player) -> sb.append(suffix).toString();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        onTriggerUnloaded();
    }

    @Override
    public void validateCondition(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onConditionLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        if (amount != null) amount.build();
    }

    @Override
    public double getAmount(final PlayerEntity player) {
        if (amount == null) return 0;

        amount.setIfRequired(CountedExpressionContext.COUNT, () -> (double)lastCount);
        amount.setCurrentNeedValue(parent, player);
        return amount.apply(player);
    }

    @Override
    public boolean test(final PlayerEntity player) {
        lastCount = 0;

        if (head) wornItems.add(player.getItemStackFromSlot(EquipmentSlotType.HEAD));
        if (chest) wornItems.add(player.getItemStackFromSlot(EquipmentSlotType.CHEST));
        if (legs) wornItems.add(player.getItemStackFromSlot(EquipmentSlotType.LEGS));
        if (feet) wornItems.add(player.getItemStackFromSlot(EquipmentSlotType.FEET));

        if (amount == null || amount.isConstant()) {
            if (items.stream().noneMatch((p) -> wornItems.stream().anyMatch(p))) return false;
            lastCount = 1;
            return true;
        }

        lastCount = items.stream().filter((p) -> wornItems.stream().anyMatch(p)).count();
        return true;
    }

    @Override
    public void validateTrigger(final Need parentNeed, final ConditionalManipulator parentCondition) throws IllegalArgumentException {
        validateCommon();
    }

    @Override
    public void onTriggerLoaded(final Need parentNeed, final ConditionalManipulator parentCondition) {
        this.parent = parentNeed;
        this.parentCondition = parentCondition;
        TickManager.INSTANCE.requestPlayerTickUpdate(this, this::asTrigger);
        if (amount != null) amount.build();
    }

    @Override
    public void onTriggerUnloaded() {
        TickManager.INSTANCE.removePlayerTickUpdate(this);
    }

    @Nullable
    @Override
    protected ExpressionContext getItemTooltipExpression(final ItemStack item) {
        amount.setIfRequired(CountedExpressionContext.COUNT, () -> 1d);
        return amount;
    }

    private void validateCommon() {
        if (!chest && !head && !legs && !feet) throw new IllegalArgumentException("At least one armor slot must be specified.");
    }

    /**
     * Called when ticking to calculate
     * @param player The player
     */
    private void asManipulator(@Nonnull final PlayerEntity player) {
        if (!test(player)) return;
        parent.adjustValue(player, getAmount(player), this);
    }

    /**
     * Called when ticking to calculate
     * @param player The player
     */
    private void asTrigger(@Nonnull final PlayerEntity player) {
        if (!test(player)) return;
        parentCondition.trigger(player, this);
    }
}

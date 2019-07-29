package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.util.RangeHelper;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;
import org.winterblade.minecraft.mods.needs.util.items.ItemIngredient;
import org.winterblade.minecraft.mods.needs.util.items.TagIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends BaseManipulator {
    @Expose
    private double defaultAmount;

    @Expose
    private boolean showTooltip;

    private int capacity = 0;

    private Map<IIngredient, Double> itemValues = new HashMap<>();

    private WeakHashMap<ItemStack, String> itemCache = new WeakHashMap<>();
    private RangeMap<Double, String> formatting = TreeRangeMap.create();

    @Override
    public void onCreated() {
        if (showTooltip) {
            DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(this::onTooltip));
        }

        // Rough string capacity for tooltips:
        capacity = parent.getName().length() + 8;
    }

    @SubscribeEvent
    public void OnItemUsed(LivingEntityUseItemEvent.Finish evt) {
        if (evt.getEntity().world.isRemote) return;

        for (Map.Entry<IIngredient, Double> item : itemValues.entrySet()) {
            if (!item.getKey().isMatch(evt.getItem())) continue;

            parent.adjustValue(evt.getEntityLiving(), item.getValue(), this);
            return;
        }
    }

    /**
     * Called on the client when Minecraft wants to render the tooltip
     * @param event The tooltip event
     */
    private void onTooltip(ItemTooltipEvent event) {
        // TODO: Consider better way of caching values:
        String msg;
        if (itemCache.containsKey(event.getItemStack())) {
            msg = itemCache.get(event.getItemStack());
        } else {
            double value = 0;
            boolean found = false;
            for (Map.Entry<IIngredient, Double> item : itemValues.entrySet()) {
                if (!item.getKey().isMatch(event.getItemStack())) continue;

                value = item.getValue();
                found = true;
                break;
            }

            if (!found) return;
            StringBuilder theLine = new StringBuilder(capacity);

            String color = formatting.get(value);
            theLine.append(color != null ? color : TextFormatting.AQUA.toString());

            theLine.append(parent.getName());
            theLine.append(": ");
            theLine.append(value);

            msg = theLine.toString();
            itemCache.put(event.getItemStack(), msg);
        }

        List<ITextComponent> toolTip = event.getToolTip();
        if (msg.isEmpty() || isThisValuePresent(toolTip)) return;
        toolTip.add(new StringTextComponent(msg));
    }

    /**
     * Determines if we've already added info about this need to the tooltip
     * @param tooltip The tooltip
     * @return True if we have, false otherwise
     */
    private boolean isThisValuePresent(List<ITextComponent> tooltip) {
        if (tooltip.size() <= 1) return false;

        for (ITextComponent line : tooltip) {
            String lineString = line.getString();
            String withoutFormatting = TextFormatting.getTextWithoutFormattingCodes(lineString);
            if (withoutFormatting == null) continue;

            if (withoutFormatting.startsWith(parent.getName())) return true;
        }
        return false;
    }

    class Deserializer implements JsonDeserializer<ItemUsedManipulator> {
        @Override
        public ItemUsedManipulator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) throw new JsonParseException("ItemUsed must be an object");

            // TODO: Get gson to heavy-lift the base class
            ItemUsedManipulator output = new ItemUsedManipulator();

            JsonObject obj = json.getAsJsonObject();
            if (!obj.has("items")) throw new JsonParseException("ItemUsed must have an items array");

            if (obj.has("defaultAmount")) output.defaultAmount = obj.get("defaultAmount").getAsInt();
            else output.defaultAmount = 1;

            if (obj.has("showTooltip")) output.showTooltip = obj.get("showTooltip").getAsBoolean();

            // Get formatting ranges:
            if (obj.has("formatting")) {
                JsonObject formattingEl = obj.getAsJsonObject("formatting");
                formattingEl.entrySet().forEach((kv) -> {
                    Range<Double> range;
                    if (kv.getValue().isJsonPrimitive()) {
                        String rangeStr = kv.getValue().getAsJsonPrimitive().getAsString();
                        range = RangeHelper.parseStringAsRange(rangeStr);
                    } else if (kv.getValue().isJsonObject()) {
                        range = RangeHelper.parseObjectToRange(kv.getValue().getAsJsonObject());
                    } else {
                        throw new JsonParseException("Format range must be an object with min/max or a string.");
                    }

                    TextFormatting format = TextFormatting.getValueByName(kv.getKey());
                    if (format == null) throw new JsonParseException("Unknown format color: " + kv.getKey());
                    output.formatting.put(range, format.toString());
                });
            } else {
                output.formatting.put(Range.greaterThan(0d), TextFormatting.GREEN.toString());
                output.formatting.put(Range.lessThan(0d), TextFormatting.RED.toString());
            }

            // Now actually deal with the items:
            JsonElement itemsEl = obj.get("items");

            // Handle if it's a key/value pair:
            if (itemsEl.isJsonObject()) {
                JsonObject itemsObj = itemsEl.getAsJsonObject();
                itemsObj.entrySet().forEach((pair) -> {
                    IIngredient ingredient = getIngredient(pair.getKey());
                    if(ingredient == null) return;

                    output.itemValues.put(ingredient, pair.getValue().getAsDouble());
                });
                return output;
            }

            if (!itemsEl.isJsonArray()) throw new JsonParseException("ItemUsed must have an items array or object");

            // Handle if it's an array:
            JsonArray itemArray = obj.getAsJsonArray("items");
            if (itemArray == null || itemArray.size() <= 0) throw new JsonParseException("ItemUsed must have an items array");

            itemArray.forEach((el) -> populateFromElement(output, el));

            return output;
        }

        /**
         * Populate the item from a single element
         * @param output    The output to add to
         * @param el        The element to parse
         */
        private void populateFromElement(ItemUsedManipulator output, JsonElement el) {
            double amount = output.defaultAmount;
            String entry;
            IIngredient ingredient = null;

            if(el.isJsonPrimitive()) {
                entry = el.getAsString();
                ingredient = getIngredient(entry);
            } else if(el.isJsonObject()) {
                JsonObject elObj = el.getAsJsonObject();

                if (elObj.has("predicate")) {
                    ingredient = getIngredient(elObj.getAsJsonPrimitive("predicate").getAsString());
                }

                if (elObj.has("amount")) {
                    amount = elObj.getAsJsonPrimitive("amount").getAsDouble();
                }
            } else {
                throw new JsonParseException("Unknown item format: " + el.toString());
            }

            if(ingredient == null) return;

            output.itemValues.put(ingredient, amount);
        }

        /**
         * Gets an ingredient from a string representation
         * @param input     The string to parse
         * @return          The parsed ingredient, or null if parsing failed.
         */
        private IIngredient getIngredient(String input) {
            if(input == null || input.isEmpty()) return null;

            if (input.startsWith("tag:")) {
                Tag<Item> tag = ItemTags.getCollection().get(new ResourceLocation(input.substring(4)));

                if (tag == null) {
                    NeedsMod.LOGGER.warn("Unknown tag " + input);
                    return null;
                }

                return new TagIngredient(tag);
            }

            Item item = RegistryManager.ACTIVE.getRegistry(Item.class).getValue(new ResourceLocation(input));
            if (item == null) {
                NeedsMod.LOGGER.warn("Unknown item " + input);
                return null;
            }

            return new ItemIngredient(new ItemStack(item));
        }
    }
}

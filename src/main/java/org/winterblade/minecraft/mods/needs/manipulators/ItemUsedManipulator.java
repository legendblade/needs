package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;
import org.winterblade.minecraft.mods.needs.util.items.ItemIngredient;
import org.winterblade.minecraft.mods.needs.util.items.TagIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends BaseManipulator {
    @Expose
    private int defaultAmount;

    private Map<IIngredient, Integer> itemValues = new HashMap<>();

    @SubscribeEvent
    public void OnItemUsed(LivingEntityUseItemEvent.Finish evt) {
        for (Map.Entry<IIngredient, Integer> item : itemValues.entrySet()) {
            if (!item.getKey().isMatch(evt.getItem())) continue;

            parent.adjustValue(evt.getEntityLiving(), item.getValue(), this);
            return;
        }
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

            JsonElement itemsEl = obj.get("items");

            // Handle if it's a key/value pair:
            if (itemsEl.isJsonObject()) {
                JsonObject itemsObj = itemsEl.getAsJsonObject();
                itemsObj.entrySet().forEach((pair) -> {
                    IIngredient ingredient = getIngredient(pair.getKey());
                    if(ingredient == null) return;

                    output.itemValues.put(ingredient, pair.getValue().getAsInt());
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
            int amount = output.defaultAmount;
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
                    amount = elObj.getAsJsonPrimitive("amount").getAsInt();
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

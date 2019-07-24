package org.winterblade.minecraft.mods.needs.manipulators;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;
import org.winterblade.minecraft.mods.needs.api.registries.ManipulatorRegistry;
import org.winterblade.minecraft.mods.needs.api.manipulators.BaseManipulator;
import org.winterblade.minecraft.mods.needs.util.items.IIngredient;
import org.winterblade.minecraft.mods.needs.util.items.ItemIngredient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@JsonAdapter(ItemUsedManipulator.Deserializer.class)
public class ItemUsedManipulator extends BaseManipulator {
    @Expose
    private int defaultAmount;

    private Map<IIngredient, Integer> itemValues = new HashMap<>();

    @Override
    public void onCreated() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void OnItemUsed(LivingEntityUseItemEvent.Finish evt) {
        for (Map.Entry<IIngredient, Integer> item : itemValues.entrySet()) {
            if (!item.getKey().IsMatch(evt.getItem())) continue;

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

            JsonArray itemArray = obj.getAsJsonArray("items");
            if (itemArray == null || itemArray.size() <= 0) throw new JsonParseException("ItemUsed must have an items array");

            itemArray.forEach((el) -> {
                if(el.isJsonPrimitive()) {
                    String entry = el.getAsString();
                    if(entry == null || entry.isEmpty()) return;

                    if (entry.startsWith("tag:")) {
                        // TODO tags
                        return;
                    }

                    if (entry.startsWith("food:")) {
                        // TODO food
                        return;
                    }

                    Item item = RegistryManager.ACTIVE.getRegistry(Item.class).getValue(new ResourceLocation(entry));
                    if (item == null) {
                        NeedsMod.LOGGER.warn("Unknown item " + entry);
                        return;
                    }

                    output.itemValues.put(new ItemIngredient(new ItemStack(item)), output.defaultAmount);
                    return;
                }
                throw new JsonParseException("Unknown item format: " + el.toString());
            });

            return output;
        }
    }
}

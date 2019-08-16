package org.winterblade.minecraft.mods.needs.util.items;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@JsonAdapter(IIngredient.Deserializer.class)
public interface IIngredient extends Predicate<ItemStack> {
    @SuppressWarnings("WeakerAccess")
    static IIngredient getIngredient(final String input) {
        if (input.startsWith("tag:")) return new TagIngredient(new ResourceLocation(input.substring(4)));

        final Item item = RegistryManager.ACTIVE.getRegistry(Item.class).getValue(new ResourceLocation(input));
        if (item == null) {
            NeedsMod.LOGGER.warn("Unknown item " + input);
            return null;
        }

        return new ItemIngredient(new ItemStack(item));
    }

    static List<IIngredient> getIngredientList(final JsonElement json) throws JsonParseException {
        if (json == null) throw new JsonParseException("Item cannot be null");
        if (json.isJsonPrimitive()) return Collections.singletonList(getIngredient(json.getAsJsonPrimitive().getAsString()));

        if (json.isJsonObject()) throw new JsonParseException("Item must either be a single item, or a list."); // TODO

        final JsonArray jsonArray = json.getAsJsonArray();
        final List<IIngredient> output = new ArrayList<>(jsonArray.size());
        jsonArray.forEach((e) -> output.add(getIngredient(e.getAsJsonPrimitive().getAsString())));

        return output;
    }

    class Deserializer implements JsonDeserializer<IIngredient> {
        @Override
        public IIngredient deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return json != null && json.isJsonPrimitive() ? getIngredient(json.getAsJsonPrimitive().getAsString()) : null;
        }
    }

    class ToListDeserializer implements JsonDeserializer<List<IIngredient>> {
        @Override
        public List<IIngredient> deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            return getIngredientList(json);
        }
    }
}

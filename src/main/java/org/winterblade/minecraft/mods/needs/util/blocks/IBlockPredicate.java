package org.winterblade.minecraft.mods.needs.util.blocks;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.IProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.RegistryManager;
import org.winterblade.minecraft.mods.needs.NeedsMod;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@JsonAdapter(IBlockPredicate.Deserializer.class)
public interface IBlockPredicate extends Predicate<BlockState>, Comparable<IBlockPredicate> {

    class Deserializer implements JsonDeserializer<IBlockPredicate> {
        @Override
        public IBlockPredicate deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            if(json.isJsonPrimitive()) {
                final String str = json.getAsString();
                return (str.startsWith("tag:")) ? new TagBlockPredicate(str) : new SimpleBlockPredicate(str);
            }
            if (!json.isJsonObject()) throw new JsonParseException("Block predicate must be a string or object");

            final JsonObject obj = json.getAsJsonObject();
            if (!obj.has("block")) throw new JsonParseException("Block predicate must contain a 'block' key.");

            final String blockString = obj.getAsJsonPrimitive("block").getAsString();
            final Block block = RegistryManager.ACTIVE.getRegistry(Block.class).getValue(new ResourceLocation(blockString));

            if (block == null) throw new JsonParseException("Unknown block: " + blockString);

            final Map<String, ? extends IProperty<?>> propertyMap = block
                    .getStateContainer()
                    .getProperties()
                    .stream()
                    .collect(Collectors.toMap(IProperty::getName, (p) -> p));

            int tests = 0;
            Predicate<BlockState> base = (state) -> state.getBlock().equals(block);

            for (final Map.Entry<String, JsonElement> pair : obj.entrySet()) {
                if (pair.getKey().equals("block")) continue;

                final IProperty<?> prop = propertyMap.get(pair.getKey());
                if (prop == null) {
                    NeedsMod.LOGGER.warn("Block " + blockString + " doesn't have a property " + pair.getKey());
                    continue;
                }

                final String val = pair.getValue().getAsString();
                final Optional<?> pv = prop.parseValue(val);
                if (!pv.isPresent()) {
                    NeedsMod.LOGGER.warn("Block " + blockString + " property " + pair.getKey() + " doesn't have value " + val);
                    continue;
                }

                base = base.and((state) -> pv.map((v) -> state.get(prop).equals(v)).orElse(false));
                tests++;
            }

            return tests <= 0 ? new SimpleBlockPredicate(block) : new BlockStatePredicate(base, tests);
        }
    }
}

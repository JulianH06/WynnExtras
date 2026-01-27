package julianh06.wynnextras.features.misc;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;
import java.util.Optional;

public class ItemStackDeserializer implements JsonDeserializer<ItemStack> {
    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE, json);
        Optional<ItemStack> stackOptional = result.result();

        if (stackOptional.isPresent()) {
            ItemStack stack = stackOptional.get();
            return stack;
        }
        return null;
    }
}

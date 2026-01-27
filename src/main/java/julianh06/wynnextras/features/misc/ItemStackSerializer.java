package julianh06.wynnextras.features.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;

public class ItemStackSerializer implements JsonSerializer<ItemStack> {
    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
//        ItemStack. RegistryWrapper.WrapperLookup;
        DataResult<JsonElement> result = src.CODEC.encodeStart(JsonOps.INSTANCE, src);
        return result.result().orElse(null);
//        Optional<NbtElement> nbtElementOptional = result.result();
//
//        if(nbtElementOptional.isPresent()) {
//            NbtElement nbt = nbtElementOptional.get();
//
//            JsonElement jsonElement = NbtToJsonConverter.convert(nbt);
//
//            return jsonElement;
//        }
////        NBTTagCompound tag = src.serializeNBT();
////        return nbtToJson(tag);
//        return null;
    }
}


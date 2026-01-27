package julianh06.wynnextras.features.misc;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

public class NbtToJsonConverter {
    public static JsonElement convert(NbtElement nbt) {
        Dynamic<NbtElement> dynamic = new Dynamic<>(NbtOps.INSTANCE, nbt);
        Dynamic<JsonElement> jsonDynamic = dynamic.convert(JsonOps.INSTANCE);
        return jsonDynamic.getValue();
    }
}

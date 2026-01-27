package julianh06.wynnextras.features.inventory.data;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;

public class BookshelfData extends BankData {
    public static BookshelfData INSTANCE = new BookshelfData();

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/" + MinecraftClient.getInstance().player.getUuid().toString() + "/bookshelf.json");
    }
}


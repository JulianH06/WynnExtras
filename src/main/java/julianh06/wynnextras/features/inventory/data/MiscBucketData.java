package julianh06.wynnextras.features.inventory.data;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class MiscBucketData extends BankData {
    public static MiscBucketData INSTANCE = new MiscBucketData();

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/miscbucket.json");
    }
}


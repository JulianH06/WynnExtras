package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class CharacterBankData extends BankData {
    public static final CharacterBankData INSTANCE = new CharacterBankData();

    @Override
    public void save() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return;
        super.save();
    }

    @Override
    public CompletableFuture<Void> saveAsync() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return CompletableFuture.completedFuture(null);
        return super.saveAsync();
    }

    @Override
    public CompletableFuture<Void> loadAsync() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return CompletableFuture.completedFuture(null);
        String characterId = BankOverlay.currentCharacterID;
        Path path = getConfigPath();
        clearData();
        return super.loadAsync(path, this.getClass(), () -> characterId.equals(BankOverlay.currentCharacterID));
    }

    @Override
    public void load() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return;
        super.load();
    }

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/" + MinecraftClient.getInstance().player.getUuid().toString() + "/characterbank_" + BankOverlay.currentCharacterID +  ".json");
    }
}

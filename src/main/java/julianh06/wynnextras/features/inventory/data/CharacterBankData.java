package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
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
    public void saveAsyncDebounced() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return;
        super.saveAsyncDebounced();
    }

    @Override
    public void load() {
        if (!BankOverlay.hasValidCurrentCharacterId()) return;
        super.load();
    }

    @Override
    public Path getConfigPath() {
        return getConfigPath(BankOverlay.currentCharacterID);
    }

    public static CompletableFuture<Void> saveLastHeldWeaponAsync(String characterId, ItemStack weapon) {
        if (!isValidCharacterId(characterId) || MinecraftClient.getInstance().player == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (characterId.equals(BankOverlay.currentCharacterID)) {
            INSTANCE.setLastHeldWeapon(weapon);
        }

        Path path = getConfigPath(characterId);
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(path.getParent());

                CharacterBankData data = null;
                if (Files.exists(path)) {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        data = BankData.getGson().fromJson(reader, CharacterBankData.class);
                    }
                }
                if (data == null) data = new CharacterBankData();
                data.setLastHeldWeapon(weapon);

                try (Writer writer = Files.newBufferedWriter(path)) {
                    BankData.getGson().toJson(data, writer);
                }
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Failed to save last held weapon: " + e.getMessage());
            }
        });
    }

    private static Path getConfigPath(String characterId) {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/" + MinecraftClient.getInstance().player.getUuid().toString() + "/characterbank_" + characterId +  ".json");
    }

    private static boolean isValidCharacterId(String characterId) {
        return characterId != null && !characterId.isBlank() && !"null".equalsIgnoreCase(characterId);
    }
}

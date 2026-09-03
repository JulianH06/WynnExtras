package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.nio.file.Path;

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

    public static void saveLastHeldWeaponAsync(String characterId, ItemStack weapon) {
        if (!isValidCharacterId(characterId) || MinecraftClient.getInstance().player == null) {
            return;
        }
        if (!characterId.equals(BankOverlay.currentCharacterID)) return;

        INSTANCE.setLastHeldWeapon(weapon);
        INSTANCE.saveAsyncDebounced();
    }

    private static Path getConfigPath(String characterId) {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/" + MinecraftClient.getInstance().player.getUuid().toString() + "/characterbank_" + characterId +  ".json");
    }

    private static boolean isValidCharacterId(String characterId) {
        return characterId != null && !characterId.isBlank() && !"null".equalsIgnoreCase(characterId);
    }
}

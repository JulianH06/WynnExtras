package julianh06.wynnextras.features.inventory.data;

import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class CharacterBankData extends BankData {
    public static CharacterBankData INSTANCE = new CharacterBankData();

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/characterbank_" + BankOverlay.currentCharacterID +  ".json");
    }
}


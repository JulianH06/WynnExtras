package julianh06.wynnextras.features.inventory.data;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class AccountBankData extends BankData {
    public static AccountBankData INSTANCE = new AccountBankData();

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras/account_bank.json");
    }
}


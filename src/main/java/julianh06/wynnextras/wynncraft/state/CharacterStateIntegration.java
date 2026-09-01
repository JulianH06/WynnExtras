package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.misc.HuntedModeTracker;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.TickScheduler;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;

@WEModule
public final class CharacterStateIntegration {
    private static String lastPassiveCharacterInfo;

    private String lastCharacterId;
    private boolean inClassSelection;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        boolean classSelection = WynncraftMenuService.isCurrent(MenuType.CLASS_SELECTION);
        if (classSelection && !inClassSelection) HuntedModeTracker.huntedMode = false;
        inClassSelection = classSelection;

        String characterId = CharacterState.id().orElse(null);
        if (characterId == null) {
            lastCharacterId = null;
            return;
        }
        if (characterId.equals(lastCharacterId)) return;
        lastCharacterId = characterId;
        onCharacterChanged(characterId);
    }

    private static void onCharacterChanged(String characterId) {
        BankOverlay.Pages = null;
        BankOverlay.currentData = null;
        BankOverlay.activeInvSlots.clear();
        BankOverlay.annotationCache.clear();
        BankOverlay.expectedOverlayType = BankOverlayType.NONE;
        BankOverlay.currentCharacterID = characterId;
        CharacterBankData.INSTANCE.load();
        CharacterState.updateCharacterInfo(
                CharacterBankData.INSTANCE.getCharacterNickname(),
                CharacterBankData.INSTANCE.getCharacterLevel());
        ProfessionOverlay.onCharacterSwap();

        String localName = CharacterState.className().orElse(null);
        int localLevel = CharacterState.level();
        if (localName != null && localLevel > 0) {
            CharacterBankData.INSTANCE.setCharacterInfo(localName, localLevel);
            CharacterBankData.INSTANCE.save();
        }
        TickScheduler.runAfterTicks(40, () -> fetchCharacterFromApi(characterId));
    }

    private static void fetchCharacterFromApi(String characterId) {
        if (MinecraftUtils.player() == null || !characterId.equals(CharacterState.id().orElse(null))) return;
        String playerName = MinecraftUtils.player().getName().getString();
        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(playerData -> {
            if (playerData == null || !characterId.equals(CharacterState.id().orElse(null))) return;
            Map<String, CharacterData> characters = playerData.getCharacters();
            if (characters == null) return;

            for (Map.Entry<String, CharacterData> entry : characters.entrySet()) {
                String apiCharId = entry.getKey().replace("-", "");
                if (!matchesCharacterId(apiCharId, characterId)) continue;
                CharacterData data = entry.getValue();
                CharacterState.updateCharacterInfo(data.getType(), data.getLevel());
                String displayName = displayName(data);
                if (displayName != null) {
                    CharacterBankData.INSTANCE.setCharacterInfo(displayName, data.getLevel(), data.getGamemode());
                    CharacterBankData.INSTANCE.save();
                }
                ProfessionOverlay.initOverflowFromApi(characterId, data);
                TickScheduler.runAfterTicks(60, ProfessionOverlay::fetchLeaderboardForAllProfessions);
                return;
            }
        }).exceptionally(error -> null);
    }

    static void savePassiveCharacterInfo(String characterId, String className, int level) {
        if (characterId == null || className == null || level <= 0) return;
        if (!characterId.equals(BankOverlay.currentCharacterID)) return;

        String characterInfo = characterId + '\0' + className + '\0' + level;
        if (characterInfo.equals(lastPassiveCharacterInfo)) return;
        lastPassiveCharacterInfo = characterInfo;
        CharacterBankData.INSTANCE.setCharacterInfo(className, level);
        CharacterBankData.INSTANCE.saveAsyncDebounced();
    }

    private static boolean matchesCharacterId(String apiId, String localId) {
        if (apiId == null || apiId.isEmpty() || localId == null || localId.isEmpty()) return false;
        int prefixLength = Math.min(8, apiId.length());
        return apiId.contains(localId) || localId.contains(apiId.substring(0, prefixLength));
    }

    private static String displayName(CharacterData data) {
        String name = data.getNickname();
        if (name == null || name.isBlank()) name = data.getReskin();
        if (name == null || name.isBlank()) name = data.getType();
        if (name == null || name.isBlank()) return null;
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}

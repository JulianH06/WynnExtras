package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.TickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = CharacterModel.class, remap = false)
public class CharacterModelMixin {
    @Shadow
    private String id;

    @Shadow
    private int level;

    @Inject(
            method = "onWorldStateChanged",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/wynntils/models/character/CharacterModel;scanCharacterInfo()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onWorldStateChanged(WorldStateEvent e, CallbackInfo ci) {
        String id = this.id;

        if (id == null || id.isEmpty() || id.equals("-")) {
            return;
        }

        BankOverlay.Pages = null;
        BankOverlay.currentData = null;
        BankOverlay.activeInvSlots.clear();
        BankOverlay.annotationCache.clear();
        BankOverlay.expectedOverlayType = BankOverlayType.NONE;

        BankOverlay.currentCharacterID = id;
        CharacterBankData.INSTANCE.load();

        // Delay to allow Wynntils to finish populating character data
        final String characterId = id;
        TickScheduler.runAfterTicks(40, () -> {
            updateCharacterInfo(characterId);
        });
    }

    private void updateCharacterInfo(String characterId) {
        try {
            // First try Wynntils local data
            String actualName = Models.Character.getActualName();
            int combatLevel = this.level;

            System.out.println("[WynnExtras] Wynntils data - Name: " + actualName + ", Level: " + combatLevel);

            // If level is 0 or name is basic, try Wynncraft API for better data
            if (combatLevel == 0 || actualName == null || actualName.isEmpty()) {
                fetchCharacterFromApi(characterId);
            } else {
                // Save Wynntils data
                CharacterBankData.INSTANCE.characterNickname = actualName;
                CharacterBankData.INSTANCE.characterLevel = combatLevel;
                CharacterBankData.INSTANCE.save();
                System.out.println("[WynnExtras] Saved character: " + actualName + " Lv." + combatLevel + " for ID: " + characterId);

                // Also try API to get Champion nickname if available
                fetchCharacterFromApi(characterId);
            }
        } catch (Exception ex) {
            System.err.println("[WynnExtras] Failed to get character info: " + ex.getMessage());
        }
    }

    private void fetchCharacterFromApi(String characterId) {
        if (McUtils.player() == null) return;

        String playerName = McUtils.player().getName().getString();
        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(playerData -> {
            if (playerData == null) return;

            Map<String, CharacterData> characters = playerData.getCharacters();
            if (characters == null) return;

            // Find matching character by ID (API uses full UUID format)
            for (Map.Entry<String, CharacterData> entry : characters.entrySet()) {
                String apiCharId = entry.getKey().replace("-", "");
                if (apiCharId.contains(characterId) || characterId.contains(apiCharId.substring(0, Math.min(8, apiCharId.length())))) {
                    CharacterData charData = entry.getValue();

                    // Build display name: Champion nickname > reskin name > base type
                    String displayName = charData.getNickname(); // Champion nickname
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = charData.getReskin(); // Reskinned class name (e.g., "knight")
                        if (displayName != null && !displayName.isEmpty()) {
                            // Capitalize first letter
                            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1).toLowerCase();
                        }
                    }
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = charData.getType(); // Base class (e.g., "WARRIOR")
                        if (displayName != null) {
                            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1).toLowerCase();
                        }
                    }

                    int apiLevel = charData.getLevel();

                    if (displayName != null && !displayName.isEmpty()) {
                        CharacterBankData.INSTANCE.characterNickname = displayName;
                        CharacterBankData.INSTANCE.characterLevel = apiLevel;
                        CharacterBankData.INSTANCE.save();
                        System.out.println("[WynnExtras] API updated character: " + displayName + " Lv." + apiLevel + " for ID: " + characterId);
                    }
                    break;
                }
            }
        });
    }
}
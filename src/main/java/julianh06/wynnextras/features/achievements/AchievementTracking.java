package julianh06.wynnextras.features.achievements;

import com.wynntils.core.components.Models;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.TickEvent;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class AchievementTracking {
    public static Achievements achievements;
    private boolean init;

    @SubscribeEvent
    private void onTick(TickEvent event) {
        if (!init) {
            init = true;
            if (getFromServer() != null) {
                achievements = getFromServer();
            }
        }
        if (achievements == null) return;

        CappedValue combatLevel = Models.CombatXp.getCombatLevel();
        int currentLevel = combatLevel.current();

        unlockLevelAchievement(currentLevel, 120);
        unlockLevelAchievement(currentLevel, 121);
    }

    private void unlockLevelAchievement(int currentLevel, int requiredLevel) {
        if (currentLevel < requiredLevel) return;

        String id = "simple.level." + requiredLevel;
        if (achievements.isUnlocked(id)) return;

        if (achievements.setCompleted(id)) {
            WynnExtras.addWynnExtrasPrefix(Text.of("Achievement Unlocked: Level " + requiredLevel));
            save();
        }
    }

    private void save() {
        Achievements.save();
    }

    private Achievements getFromServer() {
        return null;
    }

    private Achievements loadFromClient() {
        return null;
    }
}

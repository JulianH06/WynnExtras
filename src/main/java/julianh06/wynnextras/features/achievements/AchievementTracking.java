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
        if(!init){
            init = true;
            if(getFromServer() !=null){
                achievements = getFromServer();
            }
        }
        if(achievements == null){return;}
        CappedValue combatLevel = Models.CombatXp.getCombatLevel();
        int currentLevel = combatLevel.current();
//Tracking
        if(currentLevel == 120){
            boolean alr = achievements.isUnlocked("simple.level.120");
            if(!alr) {
                boolean check = achievements.setCompleted("simple.level.120");
                if (check){ WynnExtras.addWynnExtrasPrefix(Text.of("Achievement Unlocked: Level 120"));
                    save();
                };
            }
        }
        if(currentLevel == 121){
            boolean alr = achievements.isUnlocked("simple.level.121");
            if(!alr) {
                boolean check = achievements.setCompleted("simple.level.121");
                if (check){ WynnExtras.addWynnExtrasPrefix(Text.of("Achievement Unlocked: Level 121"));
                    save();
                };
            }
        }
    }
    private void save(){
        Achievements.save();
    };
    private Achievements getFromServer(){
        return null;
    }
    private Achievements loadFromClient(){return null;}

}

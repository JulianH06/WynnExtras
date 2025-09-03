package julianh06.wynnextras.mixin;

import com.wynntils.models.raid.raids.NestOfTheGrootslangsRaid;
import com.wynntils.models.raid.raids.OrphionsNexusOfLightRaid;
import com.wynntils.models.raid.raids.TheCanyonColossusRaid;
import com.wynntils.models.raid.raids.TheNamelessAnomalyRaid;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.raid.Raid;
import julianh06.wynnextras.features.raid.RaidMemberDetector;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onMouseClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if(McUtils.containerMenu() != null && slotIndex < 72 && slotIndex > 0){
            String name = McUtils.containerMenu().getSlot(slotIndex).getStack().getName().getString();
            System.out.println(name);
            if(name.contains("Nest of")) {
                RaidMemberDetector.notifyRaidSelection(Raid.NOTG);
                return;
            }

            if(name.contains("Nexus of")) {
                RaidMemberDetector.notifyRaidSelection(Raid.NOL);
                return;
            }

            if(name.contains("The Canyon")) {
                RaidMemberDetector.notifyRaidSelection(Raid.TCC);
                return;
            }

            if(name.contains("The Nameless")) {
                RaidMemberDetector.notifyRaidSelection(Raid.TNA);
                return;
            }
        }
    }
}

package julianh06.wynnextras.mixin.ShamanTotem;

import com.wynntils.core.WynntilsMod;
import com.wynntils.models.abilities.ShamanTotemModel;
import com.wynntils.models.abilities.event.TotemEvent;
import com.wynntils.models.abilities.type.ShamanTotem;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin (value = ShamanTotemModel.class, remap = false)
public class ShamanTotemModelMixin {
    @Shadow
    @Final
    private Integer[] timerlessTotemVisibleIds;

    @Shadow
    @Final
    private Map<Integer, Integer> orphanedTimers;

    @Shadow
    @Final
    private ShamanTotem[] totems;

    @Shadow
    @Final
    private static double TOTEM_SEARCH_RADIUS = 20.0;


    @Inject(method = "findAndLinkTotem", at = @At("HEAD"), remap = false, cancellable = true)
    void findAndLinkTotem(int timerId, int parsedTime, DisplayEntity.TextDisplayEntity textDisplay, CallbackInfo ci) {
        //if(!SimpleConfig.getConfigHolder(WynnExtrasConfig.class).get().totemRangeVisualizerToggle) return;

        assert McUtils.mc().world != null;
        List<ArmorStandEntity> possibleTotems = McUtils.mc().world.getNonSpectatingEntities(ArmorStandEntity.class, new Box(
                textDisplay.getPos().x - TOTEM_SEARCH_RADIUS,
                textDisplay.getPos().y - TOTEM_SEARCH_RADIUS,
                textDisplay.getPos().z - TOTEM_SEARCH_RADIUS,
                textDisplay.getPos().x + TOTEM_SEARCH_RADIUS,
                textDisplay.getPos().y + TOTEM_SEARCH_RADIUS * 5,
                textDisplay.getPos().z + TOTEM_SEARCH_RADIUS));

        for (ArmorStandEntity possibleTotem : possibleTotems) {
            for (int i = 0; i < this.timerlessTotemVisibleIds.length; ++i) {
                if (this.timerlessTotemVisibleIds[i] != null && possibleTotem.getId() == this.timerlessTotemVisibleIds[i]) {
                    ShamanTotem totem = this.totems[i];
                    totem.setTimerEntityId(timerId);
                    totem.setTime(parsedTime);
                    totem.setPosition(possibleTotem.getPos());
                    totem.setState(ShamanTotem.TotemState.ACTIVE);
                    WynntilsMod.postEvent(new TotemEvent.Activated(totem.getTotemNumber(), possibleTotem.getPos()));
                    this.timerlessTotemVisibleIds[i] = null;
                    if (this.orphanedTimers.containsKey(timerId) && this.orphanedTimers.get(timerId) > 1) {
                        WynntilsMod.info("Matched an orphaned totem timer " + timerId + " to a totem " + totem.getTotemNumber() + " after " + String.valueOf(this.orphanedTimers.get(timerId)) + " attempts.");
                        this.orphanedTimers.remove(timerId);
                    }

                    return;
                }
            }
        }

        this.orphanedTimers.merge(timerId, 1, Integer::sum);
        if (this.orphanedTimers.get(timerId) == 2) {
            WynntilsMod.warn("Matched an unbound totem timer " + timerId + " but couldn't find a totem to bind it to.");
        }

        ci.cancel();
    }
}

package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.abilities.ShamanTotemModel;
import com.wynntils.models.abilities.type.ShamanTotem;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Objects;

@WEModule
public class ShamanTotemCircle {
    private static WynnExtrasConfig config;


    public static HashMap<Integer, WEVec> totemPositions = new HashMap<>();

    public Integer[] timerlessTotemVisibleIds = new Integer[4];

    public ShamanTotem[] totems = new ShamanTotem[4];

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }
//        if (Models.Character.getClassType() != ClassType.SHAMAN) return;
//        if(MinecraftClient.getInstance().world == null) return;
//        java.util.List<ArmorStandEntity> possibleTotems = McUtils.mc().world.
//                getNonSpectatingEntities(
//                        ArmorStandEntity.class,
//                        new Box(McUtils.player().getPos().x - 1.0,
//                                McUtils.player().getPos().y - 1.0,
//                                McUtils.player().getPos().z - 1.0,
//                                McUtils.player().getPos().x + 1.0,
//                                McUtils.player().getPos().y + 5.0,
//                                McUtils.player().getPos().z + 1.0));
//
//        for (ArmorStandEntity possibleTotem : possibleTotems) {
//            //for (int i = 0; i < timerlessTotemVisibleIds.length; i++) {
//            System.out.println(Objects.requireNonNull(possibleTotem.getDisplayName()).getString());
//
////                if (timerlessTotemVisibleIds[i] != null && possibleTotem.getId() == timerlessTotemVisibleIds[i]) {
////                    // we found the totem that this timer belongs to, bind it
////                    ShamanTotem totem = totems[i];
////
////                    totem.setTimerEntityId(timerId);
////                    totem.setTime(parsedTime);
////                    totem.setPosition(possibleTotem.position());
////                    totem.setState(ShamanTotem.TotemState.ACTIVE);
////
////                    WynntilsMod.postEvent(new TotemEvent.Activated(totem.getTotemNumber(), possibleTotem.position()));
////
////                    timerlessTotemVisibleIds[i] = null;
////                    if (orphanedTimers.containsKey(timerId) && orphanedTimers.get(timerId) > 1) {
////                        WynntilsMod.info("Matched an orphaned totem timer " + timerId + " to a totem "
////                                + totem.getTotemNumber() + " after " + orphanedTimers.get(timerId) + " attempts.");
////                        orphanedTimers.remove(timerId);
////                    }
////
////                    return;
////                }
//            //}
//        }

        if(!config.totemRangeVisualizerToggle) return;
        for (int i = 0; i < 4; i++) {
            if(totemPositions.containsKey(i)) {
                Color totemColor;
                CustomColor configTotemColor = CustomColor.fromChatFormatting(Objects.requireNonNull(Formatting.byName(config.totemColor)));
                int red = configTotemColor.r;
                int green = configTotemColor.g;
                int blue = configTotemColor.b;
                totemColor = new Color(red, green, blue);

                Color eldritchCallColor;
                CustomColor configeldritchCallColor = CustomColor.fromChatFormatting(Objects.requireNonNull(Formatting.byName(config.eldritchCallColor)));
                int red2 = configeldritchCallColor.r;
                int green2 = configeldritchCallColor.g;
                int blue2 = configeldritchCallColor.b;
                eldritchCallColor = new Color(red2, green2, blue2);

                WorldRenderUtils.draw3DCircle(event, totemPositions.get(i).add(0, -1.5, 0), config.totemRange, totemColor, 4, true);
                WorldRenderUtils.draw3DCircle(event, totemPositions.get(i).add(0, -1.5, 0), config.eldritchCallRange, eldritchCallColor, 4, true);
            }
        }
    }
}
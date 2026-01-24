package julianh06.wynnextras.features.misc;

import com.wynntils.models.abilities.type.ShamanTotem;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;
import java.util.HashMap;

@WEModule
public class ShamanTotemCircle {
    private static WynnExtrasConfig config;


    public static HashMap<Integer, WEVec> totemPositions = new HashMap<>();

    public Integer[] timerlessTotemVisibleIds = new Integer[4];

    public ShamanTotem[] totems = new ShamanTotem[4];

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
        }

        if(!config.totemRangeVisualizerToggle) return;

        for (int i = 0; i < 4; i++) {
            if(totemPositions.containsKey(i)) {
                Color totemColor;
                CustomColor configTotemColor = CustomColor.fromChatFormatting(config.totemColor.getFormatting());
                int red = configTotemColor.r();
                int green = configTotemColor.g();
                int blue = configTotemColor.b();
                totemColor = new Color(red, green, blue);

                Color eldritchCallColor;
                CustomColor configeldritchCallColor = CustomColor.fromChatFormatting(config.eldritchCallColor.getFormatting());
                int red2 = configeldritchCallColor.r();
                int green2 = configeldritchCallColor.g();
                int blue2 = configeldritchCallColor.b();
                eldritchCallColor = new Color(red2, green2, blue2);

                WorldRenderUtils.draw3DCircle(event, totemPositions.get(i).add(0, -1.5, 0), config.totemRange, totemColor, 4, true);
                WorldRenderUtils.draw3DCircle(event, totemPositions.get(i).add(0, -1.5, 0), config.eldritchCallRange, eldritchCallColor, 4, true);
            }
        }
    }
}
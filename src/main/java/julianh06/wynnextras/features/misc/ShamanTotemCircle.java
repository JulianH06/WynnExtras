package julianh06.wynnextras.features.misc;

import com.wynntils.models.abilities.type.ShamanTotem;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WERenderLayers;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.client.render.BufferBuilder;
import net.neoforged.bus.api.SubscribeEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Objects;

@WEModule
public class ShamanTotemCircle {
    private static WynnExtrasConfig config;

    public static HashMap<Integer, WEVec> totemPositions = new HashMap<>();

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
        }

        if (WorldRenderUtils.INSTANCE_SHAMANTOTEM.buffer == null) {
            WorldRenderUtils.INSTANCE_SHAMANTOTEM.buffer = new BufferBuilder(WorldRenderUtils.allocator, WERenderLayers.linePipeline.getVertexFormatMode(), WERenderLayers.linePipeline.getVertexFormat());
        }

        if(!config.totemRangeVisualizerToggle) return;
        for (int i = 0; i < 4; i++) {
            if(totemPositions.containsKey(i)) {
                Color totemColor;
                CustomColor configTotemColor = CustomColor.fromChatFormatting(Objects.requireNonNull(config.totemColor.getFormatting()));
                int red = configTotemColor.r();
                int green = configTotemColor.g();
                int blue = configTotemColor.b();
                totemColor = new Color(red, green, blue);

                Color eldritchCallColor;
                CustomColor configeldritchCallColor = CustomColor.fromChatFormatting(Objects.requireNonNull(config.eldritchCallColor.getFormatting()));
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
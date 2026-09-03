package julianh06.wynnextras.utils;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;

import java.util.List;

public final class TooltipUtils {
    private TooltipUtils() {}

    public static List<TooltipComponent> getClientTooltipComponent(List<Text> lines) {
        if (lines == null || lines.isEmpty()) return List.of();
        return lines.stream().map(Text::asOrderedText).map(TooltipComponent::of).toList();
    }

    public static int getTooltipHeight(List<TooltipComponent> components) {
        if (components == null || components.isEmpty()) return 0;
        TextRenderer renderer = MinecraftUtils.mc().textRenderer;
        int height = 0;
        for (TooltipComponent component : components) height += component.getHeight(renderer);
        return height;
    }

}

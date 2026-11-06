package julianh06.wynnextras.features.buildplanner.gui;

import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector2i;
import org.joml.Vector2ic;

final class PlannerTooltip {
    private static final int SCREEN_MARGIN = 4;

    private PlannerTooltip() {}

    static void render(DrawContext context, TextRenderer renderer, List<Text> lines, Identifier style,
            int mouseX, int mouseY, int screenWidth, int screenHeight, float plannerScale) {
        if (lines == null || lines.isEmpty()) return;

        List<TooltipComponent> components = lines.stream()
                .map(Text::asOrderedText)
                .map(TooltipComponent::of)
                .toList();
        int width = components.stream().mapToInt(component -> component.getWidth(renderer)).max().orElse(0);
        int height = (components.size() == 1 ? -2 : 0)
                + components.stream().mapToInt(component -> component.getHeight(renderer)).sum();
        int viewportWidth = Math.round(screenWidth * plannerScale);
        int viewportHeight = Math.round(screenHeight * plannerScale);
        float scale = Math.min(1.0F, Math.min(
                (viewportWidth - SCREEN_MARGIN * 2.0F) / Math.max(1, width),
                (viewportHeight - SCREEN_MARGIN * 2.0F) / Math.max(1, height + 3)));

        context.getMatrices().pushMatrix();
        try {
            float renderScale = scale / plannerScale;
            context.getMatrices().scale(renderScale, renderScale);
            context.drawTooltipImmediately(renderer, components,
                    Math.round(mouseX * plannerScale), Math.round(mouseY * plannerScale),
                    new FittingPositioner(scale, viewportWidth, viewportHeight), style);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private record FittingPositioner(float scale, int screenWidth, int screenHeight)
            implements TooltipPositioner {
        @Override
        public Vector2ic getPosition(int ignoredWidth, int ignoredHeight, int mouseX, int mouseY,
                int tooltipWidth, int tooltipHeight) {
            int scaledWidth = Math.round(tooltipWidth * scale);
            int scaledHeight = Math.round((tooltipHeight + 3) * scale);
            int x = mouseX + 12;
            int y = mouseY - 12;

            if (x + scaledWidth > screenWidth - SCREEN_MARGIN) {
                x = mouseX - 12 - scaledWidth;
            }
            x = Math.clamp(x, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenWidth - scaledWidth - SCREEN_MARGIN));
            y = Math.clamp(y, SCREEN_MARGIN, Math.max(SCREEN_MARGIN, screenHeight - scaledHeight - SCREEN_MARGIN));
            return new Vector2i(Math.round(x / scale), Math.round(y / scale));
        }
    }
}

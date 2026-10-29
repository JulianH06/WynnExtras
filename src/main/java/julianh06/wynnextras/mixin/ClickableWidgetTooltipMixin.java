package julianh06.wynnextras.mixin;

import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetTooltipMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/tooltip/TooltipState;render("
                    + "Lnet/minecraft/client/gui/DrawContext;IIZZLnet/minecraft/client/gui/ScreenRect;)V"))
    private void renderScaledTooltip(TooltipState tooltip, DrawContext context, int mouseX, int mouseY,
            boolean hovered, boolean focused, ScreenRect bounds) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof WEScreen)) {
            tooltip.render(context, mouseX, mouseY, hovered, focused, bounds);
            return;
        }
        Vector2f position = context.getMatrices().transformPosition(mouseX, mouseY, new Vector2f());
        tooltip.render(context, Math.round(position.x), Math.round(position.y), hovered, focused,
                bounds.transform(context.getMatrices()));
    }
}

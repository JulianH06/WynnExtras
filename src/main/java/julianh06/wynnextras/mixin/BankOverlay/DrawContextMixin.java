package julianh06.wynnextras.mixin.BankOverlay;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (DrawContext.class)
public abstract class DrawContextMixin{

    @Shadow public abstract MatrixStack getMatrices();

    @Redirect(
            method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V")
    )
    public void redirectTranslate(MatrixStack instance, float x, float y, float z) {
        if(BankOverlay.currentOverlayType == BankOverlayType.NONE) {
            instance.translate(0.0F, 0.0F, 400.0F);
        } else {
            instance.translate(0.0f, 0.0f, 501f);
        }
    }

    @Redirect(
            method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/tooltip/TooltipBackgroundRenderer;render(Lnet/minecraft/client/gui/DrawContext;IIIIILnet/minecraft/util/Identifier;)V")
    )
    private static void redirectRender(DrawContext context, int x, int y, int width, int height, int z, Identifier texture) {
        if(BankOverlay.currentOverlayType == BankOverlayType.NONE) {
            TooltipBackgroundRenderer.render(context, x, y, width, height, 400, texture);
        } else {
            TooltipBackgroundRenderer.render(context, x, y, width, height, 500, texture);
        }
    }

    @Inject(method = "drawStackCount", at = @At("HEAD"), cancellable = true)
    private void drawStackCount(TextRenderer textRenderer, ItemStack stack, int x, int y, String stackCountText, CallbackInfo ci) {
        if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
            if (stack.getCount() != 1 || stackCountText != null) {
                String string = stackCountText == null ? String.valueOf(stack.getCount()) : stackCountText;
                this.getMatrices().push();
                this.getMatrices().translate(0.0F, 0.0F, 200.0F);
                FontRenderer.getInstance().renderText(
                        this.getMatrices(),
                        StyledText.fromString(string),
                        (float) x + 19 - 2 - textRenderer.getWidth(string),
                        (float) y + 6 + 3,
                        CustomColor.fromHexString("FFFFFF"),
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL,
                        1.0f
                );
                this.getMatrices().pop();
                ci.cancel();
            }
        }
    }
}

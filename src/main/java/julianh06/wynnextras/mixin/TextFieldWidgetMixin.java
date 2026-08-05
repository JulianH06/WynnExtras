package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void consumeShoppingListEditorCharacter(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen
                && ShoppingListMenuExtension.isEditorTextInputFocused()) {
            cir.setReturnValue(true);
        }
    }
}

package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.chat.ChatPeek;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatHud.class)
public abstract class ChatHudFocusedMixin {
    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void forceFocusedHeight(CallbackInfoReturnable<Integer> cir) {
        if (!WynnExtrasConfig.INSTANCE.chatPeekEnabled) return;

        if (ChatPeek.isPeeking) {
            MinecraftClient client = MinecraftClient.getInstance();
            double focused = client.options.getChatHeightFocused().getValue();

            cir.setReturnValue(ChatHud.getHeight(focused));
        }
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V",
            at = @At("HEAD"),
            index = 4,
            argsOnly = true
    )
    private boolean forceExpandedWhenPeeking(boolean value) {
        if (!WynnExtrasConfig.INSTANCE.chatPeekEnabled) return value;

        return ChatPeek.isPeeking || value;
    }
}
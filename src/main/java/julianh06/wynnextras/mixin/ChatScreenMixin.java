package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.chat.ChatManager;
import julianh06.wynnextras.features.chat.mediapreview.ChatMediaPreview;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderProfessionOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ProfessionOverlay.renderOnScreen(context);
        ChatMediaPreview.render(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMediaPreviewClick(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ChatMediaPreview.mouseClicked(click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void consumeShoppingListEditorKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (ShoppingListMenuExtension.isEditorTextInputFocused()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (message == null || message.isEmpty() || ChatManager.getCurrentChannel() == ChatManager.ChatChannel.ALL) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if(message.matches("^/a\\s.*")) {
            message = message.substring(2);
            player.networkHandler.sendChatMessage(message);
            mc.inGameHud.getChatHud().addToMessageHistory(message);
            ci.cancel();
            return;
        }
        if(message.matches("^/ac\\s.*")) {
            message = message.substring(3);
            player.networkHandler.sendChatMessage(message);
            mc.inGameHud.getChatHud().addToMessageHistory(message);
            ci.cancel();
            return;
        }

        if (message.startsWith("/")) return;

        String processed = ChatManager.processMessageForSend(message);
        player.networkHandler.sendChatMessage(processed);

        if (addToHistory) {
            mc.inGameHud.getChatHud().addToMessageHistory(message);
        }

        ci.cancel();
    }
}

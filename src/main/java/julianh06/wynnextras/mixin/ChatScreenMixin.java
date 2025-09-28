package julianh06.wynnextras.mixin;

import julianh06.wynnextras.features.chat.ChatManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (message == null || message.isEmpty()) return;

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

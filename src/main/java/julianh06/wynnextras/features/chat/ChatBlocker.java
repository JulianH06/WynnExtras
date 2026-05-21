package julianh06.wynnextras.features.chat;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

@WEModule
public class ChatBlocker {
    public ChatBlocker() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                !shouldBlock(message));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || !shouldBlock(message));
    }

    private static boolean shouldBlock(Text message) {
        List<String> blockedWords = WynnExtrasConfig.INSTANCE.blockedWords;
        if (blockedWords == null || blockedWords.isEmpty()) return false;

        String msgLower = message.getString().toLowerCase(Locale.ROOT);
        for (String blockedWord : blockedWords) {
            if (blockedWord == null || blockedWord.isBlank()) continue;
            if (msgLower.contains(blockedWord.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}

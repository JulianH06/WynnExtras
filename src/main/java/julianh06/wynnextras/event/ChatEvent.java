package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import julianh06.wynnextras.features.chat.ChatManager;

public class ChatEvent extends WEEvent {
    private static boolean registered = false;
    public Text message;

    public ChatEvent(Text Message) {
        this.message = Message;
    }

    public static void register() {
        if (registered) return;
        registered = true;

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                new ChatEvent(message).post());
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                new ChatEvent(message).post());
    }

    public Text getProcessedMessage() {
        return Text.literal(ChatManager.processMessageForSend(message.getString()));
    }
}

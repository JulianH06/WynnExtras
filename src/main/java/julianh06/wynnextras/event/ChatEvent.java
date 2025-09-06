package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.minecraft.text.Text;
import julianh06.wynnextras.features.chat.ChatManager;

public class ChatEvent extends WEEvent {
    public Text message;

    public ChatEvent(Text Message) {
        this.message = Message;
    }

   public Text getProcessedMessage() {
         return Text.literal(ChatManager.processMessageForSend(message.getString()));
     }
}

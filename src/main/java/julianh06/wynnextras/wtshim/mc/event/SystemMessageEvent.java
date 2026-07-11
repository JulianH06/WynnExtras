// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — SystemMessageEvent stub. */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.text.Text;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SystemMessageEvent extends Event implements ICancellableEvent {
    private Text message;
    public SystemMessageEvent(Text message) { this.message = message; }
    public Text getMessage() { return message; }
    public void setMessage(Text m) { this.message = m; }

    public static class ChatReceivedEvent extends SystemMessageEvent {
        public ChatReceivedEvent(Text message) { super(message); }
    }
}

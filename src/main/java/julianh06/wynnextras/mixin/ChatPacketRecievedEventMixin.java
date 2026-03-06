package julianh06.wynnextras.mixin;

import com.wynntils.mc.event.SystemMessageEvent.ChatReceivedEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.features.chat.ChatNotificator;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SystemMessageEvent.ChatReceivedEvent.class)
public class ChatPacketRecievedEventMixin {
    // ChatEvent is already posted via ClientReceiveMessageEvents.GAME in ClientEvents.java
    // Removed duplicate posting that caused double notifications
}

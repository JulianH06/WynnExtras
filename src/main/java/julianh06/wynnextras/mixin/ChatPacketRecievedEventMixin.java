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
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    public void started (Text message, CallbackInfo ci) {
        new ChatEvent(message).post();
        //ChatNotificator.onPlayerChatReceived(message);
    }
}

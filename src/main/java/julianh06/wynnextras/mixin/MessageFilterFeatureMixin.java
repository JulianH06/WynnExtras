package julianh06.wynnextras.mixin;

import com.wynntils.features.chat.MessageFilterFeature;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (MessageFilterFeature.class)
public class MessageFilterFeatureMixin {
    @Unique
    private static WynnExtrasConfig config;

    @Inject(method = "onMessage", at = @At("TAIL"), remap = false)
    void blockMessage(ChatMessageReceivedEvent e, CallbackInfo ci) {

        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }

 String msgLower = e.getStyledText().getString().toLowerCase();

        if (msgLower.contains("to the tower! [")) {
                e.setCanceled(true);
                return;
            }

        for(String blockedWord : config.blockedWords) {
            if (msgLower.contains(blockedWord.toLowerCase())) {
                e.setCanceled(true);
            }
        }
    }
}

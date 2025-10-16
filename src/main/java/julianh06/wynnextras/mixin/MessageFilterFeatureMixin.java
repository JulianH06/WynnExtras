package julianh06.wynnextras.mixin;

import com.wynntils.features.chat.MessageFilterFeature;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.regex.Pattern;
@Mixin(MessageFilterFeature.class)
public class MessageFilterFeatureMixin {
    @Unique
    private static WynnExtrasConfig config;


    @Inject(method = "onMessage", at = @At("TAIL"), remap = false)
    void blockMessage(ChatMessageEvent.Match e, CallbackInfo ci) {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }

        String raw = e.getMessage().withoutFormatting().getString();
        String msgLower = raw.toLowerCase(Locale.ROOT);

        if (!msgLower.contains(": ")) {
            for (Pattern pattern : RaidChatNotifier.BLOCKED_PATTERNS) {
                if (pattern.matcher(msgLower).find()
                        && !msgLower.contains("[wynnextras]")
                        && config.toggleRaidTimestamps) {
                    RaidChatNotifier.handleMessage(e.getMessage().withoutFormatting().getString());
                    e.setCanceled(true);
                    return;
                }
            }
        }

        for (String blockedWord : config.blockedWords) {
            if (msgLower.contains(blockedWord.toLowerCase())) {
                e.setCanceled(true);
            }
        }
    }
}

package julianh06.wynnextras.features.chat;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class ChatBlocker {
    @SubscribeEvent
    public void onChatDirect(ChatEvent event) {
        String msgLower = event.message.getString().toLowerCase();
        for (String blockedWord : WynnExtrasConfig.INSTANCE.blockedWords) {
            if (msgLower.contains(blockedWord.toLowerCase())) {
                event.setCanceled(true); //TODO: doesnt work, find other solution
            }
        }
    }
}
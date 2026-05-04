package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.DisconnectEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class HuntedModeTracker {

    public static boolean huntedMode = false;

    private static final String MSG_CURRENTLY_HUNTED = "You are currently in hunted mode (PvP on)!";
    private static final String MSG_HUNTED_ON  = "You have enabled hunted mode";
    private static final String MSG_HUNTED_OFF = "You have disabled hunted mode";

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String msg = event.message.getString();
        if (msg.contains(MSG_CURRENTLY_HUNTED) || msg.contains(MSG_HUNTED_ON)) {
            huntedMode = true;
        } else if (msg.contains(MSG_HUNTED_OFF)) {
            huntedMode = false;
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        huntedMode = false;
    }

    @SubscribeEvent
    public void onDisconnect(DisconnectEvent event) {
        huntedMode = false;
    }
}

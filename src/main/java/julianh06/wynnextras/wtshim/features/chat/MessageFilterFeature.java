// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — MessageFilterFeature stand-in.
 *
 * Mixin target: WynnExtras' MessageFilterFeatureMixin @Injects the TAIL of
 * onMessage(ChatMessageEvent.Match) to run its filter/notifier logic. Our body is
 * intentionally empty — the mixin does the work. WynntilsCompatInit wires Minecraft's
 * chat events to actually call this method so the tail inject fires.
 */
package julianh06.wynnextras.wtshim.features.chat;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.handlers.chat.event.ChatMessageEvent;

public class MessageFilterFeature extends Feature {
    public void onMessage(ChatMessageEvent.Match e) {
        // Intentionally empty. WynnExtras injects at TAIL.
    }
}

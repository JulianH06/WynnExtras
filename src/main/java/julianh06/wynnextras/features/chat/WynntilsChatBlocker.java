package julianh06.wynnextras.features.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Listens to {@link ChatMessageEvent.Match} directly on Wynntils's event bus and
 * cancels chat lines that match a {@link RaidChatNotifier#BLOCKED_PATTERNS} entry.
 *
 * This is independent of Wynntils's MessageFilterFeature being enabled — the previous
 * approach mixed into MessageFilterFeature.onMessage which silently no-ops when that
 * feature is disabled, so the raw raid messages still leaked through.
 */
public final class WynntilsChatBlocker {
    private static final WynntilsChatBlocker INSTANCE = new WynntilsChatBlocker();

    public static void register() {
        WynntilsMod.registerEventListener(INSTANCE);
    }

    @SubscribeEvent
    public void onMatch(ChatMessageEvent.Match event) {
        if (!WynnExtrasConfig.INSTANCE.toggleRaidTimestamps) return;
        String raw = event.getMessage().withoutFormatting().getString();
        String msgLower = raw.toLowerCase(Locale.ROOT);
        if (msgLower.contains(": ")) return;             // ignore player chat
        if (msgLower.contains("[wynnextras]")) return;   // never cancel our own output
        for (Pattern pattern : RaidChatNotifier.BLOCKED_PATTERNS) {
            if (pattern.matcher(msgLower).find()) {
                event.cancelChat();
                return;
            }
        }
    }
}

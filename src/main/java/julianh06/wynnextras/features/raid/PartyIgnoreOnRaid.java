package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.wynncraft.state.PartyState;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.api.WEEventBus;
import julianh06.wynnextras.utils.TickScheduler;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyIgnoreOnRaid {
    private static final int MAX_PARTY_REFRESH_ATTEMPTS = 3;
    private static final Set<String> autoIgnoredThisRaid = new LinkedHashSet<>();
    private static final Set<String> trackedIgnored = new LinkedHashSet<>();

    private static final Pattern IGNORE_ADDED =
            Pattern.compile("([A-Za-z0-9_]{3,16}) has been added to your ignore list");
    private static final Pattern IGNORE_REMOVED =
            Pattern.compile("([A-Za-z0-9_]{3,16}) has been removed from your ignore list");
    private static final Pattern FORMAT_CODE = Pattern.compile("§[0-9a-fk-or]");

    public static void register() {
        WEEventBus.registerEventListener(new PartyIgnoreOnRaid());
    }

    public static void onRaidStarted() {
        if (!WynnExtrasConfig.INSTANCE.autoIgnorePartyInRaid) return;
        PartyState.requestRefresh();
        TickScheduler.runAfterTicks(40, () -> ignoreCurrentParty(MAX_PARTY_REFRESH_ATTEMPTS));
    }

    private static void ignoreCurrentParty(int attemptsRemaining) {
        if (!WynnExtrasConfig.INSTANCE.autoIgnorePartyInRaid) return;
        if (PartyState.isStale()) {
            if (attemptsRemaining <= 1) return;
            PartyState.requestRefresh();
            TickScheduler.runAfterTicks(20, () -> ignoreCurrentParty(attemptsRemaining - 1));
            return;
        }
        List<String> members = PartyState.members();
        if (members == null || members.isEmpty()) return;
        String self = MinecraftUtils.playerName();
        int count = 0;
        for (String name : members) {
            if (name == null || name.isEmpty()) continue;
            if (self != null && name.equalsIgnoreCase(self)) continue;
            PartyState.sendCommand("ignore add " + name);
            autoIgnoredThisRaid.add(name);
            count++;
        }
        if (count > 0) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("§7Auto-ignored " + count + " party " + (count == 1 ? "member" : "members") + " for this raid.")));
        }
    }

    @SubscribeEvent
    public void onRaidEnd(RaidEndedEvent event) {
        if (autoIgnoredThisRaid.isEmpty()) return;
        int count = autoIgnoredThisRaid.size();
        for (String name : autoIgnoredThisRaid) {
            PartyState.sendCommand("ignore remove " + name);
        }
        autoIgnoredThisRaid.clear();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                Text.of("§7Un-ignored " + count + " party " + (count == 1 ? "member" : "members") + ".")));
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String message = event.message.getString();
        if (!message.contains("ignore list")) return;

        String raw = FORMAT_CODE.matcher(message).replaceAll("");
        Matcher added = IGNORE_ADDED.matcher(raw);
        if (added.find()) trackedIgnored.add(added.group(1));
        Matcher removed = IGNORE_REMOVED.matcher(raw);
        if (removed.find()) trackedIgnored.remove(removed.group(1));
    }

    public static Set<String> getTrackedIgnored() {
        return trackedIgnored;
    }
}

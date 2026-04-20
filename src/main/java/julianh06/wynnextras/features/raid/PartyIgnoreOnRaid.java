package julianh06.wynnextras.features.raid;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
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
    private static final Set<String> autoIgnoredThisRaid = new LinkedHashSet<>();
    private static final Set<String> trackedIgnored = new LinkedHashSet<>();

    private static final Pattern IGNORE_ADDED =
            Pattern.compile("([A-Za-z0-9_]{3,16}) has been added to your ignore list");
    private static final Pattern IGNORE_REMOVED =
            Pattern.compile("([A-Za-z0-9_]{3,16}) has been removed from your ignore list");

    public static void register() {
        WEEventBus.registerEventListener(new PartyIgnoreOnRaid());
    }

    public static void onRaidStarted() {
        if (!WynnExtrasConfig.INSTANCE.autoIgnorePartyInRaid) return;
        // /party list is already queued in RaidStartEventMixin; wait for Wynntils to parse it.
        TickScheduler.runAfterTicks(40, PartyIgnoreOnRaid::ignoreCurrentParty);
    }

    private static void ignoreCurrentParty() {
        if (!WynnExtrasConfig.INSTANCE.autoIgnorePartyInRaid) return;
        List<String> members;
        try { members = Models.Party.getPartyMembers(); } catch (Exception e) { return; }
        if (members == null || members.isEmpty()) return;
        String self = McUtils.playerName();
        int count = 0;
        for (String name : members) {
            if (name == null || name.isEmpty()) continue;
            if (self != null && name.equalsIgnoreCase(self)) continue;
            Handlers.Command.queueCommand("ignore add " + name);
            autoIgnoredThisRaid.add(name);
            count++;
        }
        if (count > 0) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("§7Auto-ignored " + count + " party " + (count == 1 ? "member" : "members") + " for this raid.")));
        }
    }

    @SubscribeEvent
    public void onRaidEnd(RaidEndedEvent event) {
        if (autoIgnoredThisRaid.isEmpty()) return;
        int count = autoIgnoredThisRaid.size();
        for (String name : autoIgnoredThisRaid) {
            Handlers.Command.queueCommand("ignore remove " + name);
        }
        autoIgnoredThisRaid.clear();
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                Text.of("§7Un-ignored " + count + " party " + (count == 1 ? "member" : "members") + ".")));
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String raw = event.message.getString().replaceAll("§[0-9a-fk-or]", "");
        Matcher added = IGNORE_ADDED.matcher(raw);
        if (added.find()) trackedIgnored.add(added.group(1));
        Matcher removed = IGNORE_REMOVED.matcher(raw);
        if (removed.find()) trackedIgnored.remove(removed.group(1));
    }

    public static Set<String> getTrackedIgnored() {
        return trackedIgnored;
    }
}

// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * SLIM: tracks the party member list (the sole thing WynnExtras reads via
 * PartyIgnoreOnRaid#getPartyMembers). Chat-driven (ChatMessageEvent.Match) party
 * membership tracking + "/party list" request/parse, faithful to Wynntils.
 * Dropped (no WynnExtras caller): Hades relations, PartyEvent posting, the party
 * scoreboard part / sbPartyMembers, SetPlayerTeamEvent offline tracking, member
 * priorities, and StyledTextUtils.extractNameAndNick nick resolution (member names
 * are taken directly from the chat capture group — nicknamed joins may differ until
 * the next "/party list", which is the authoritative population path).
 */
package julianh06.wynnextras.wtshim.models.party;

import julianh06.wynnextras.wtshim.core.WynntilsMod;
import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.handlers.chat.event.ChatMessageEvent;
import julianh06.wynnextras.wtshim.models.worlds.event.WorldStateEvent;
import julianh06.wynnextras.wtshim.models.worlds.type.WorldState;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.mc.StyledTextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * This model handles the player's party relations (slim: member list only).
 */
public final class PartyModel extends Model {
    //  is for the first line,  is for the other lines (injected from source).
    private static final String PARTY_PREFIX_REGEX = "§e(?:\uE005\uE002|\uE001) ";

    private static final Pattern PARTY_LIST_ALL = Pattern.compile(PARTY_PREFIX_REGEX + "Party members: (.*)");
    private static final Pattern PARTY_LIST_LEADER = Pattern.compile("§b([a-zA-Z0-9_]+)");

    private static final Pattern PARTY_COMMAND_FAILED =
            Pattern.compile(PARTY_PREFIX_REGEX + "You must be in a party to use this\\.");

    private static final Pattern PARTY_PLAYER_LEFT =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have left your current party");
    private static final Pattern PARTY_PLAYER_KICKED =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have been kicked from your party");
    private static final Pattern PARTY_PLAYER_DISBANDED =
            Pattern.compile(PARTY_PREFIX_REGEX + "Your party has been disbanded");

    private static final Pattern PARTY_PLAYER_CREATED =
            Pattern.compile(PARTY_PREFIX_REGEX + "You have successfully created a party\\.");
    private static final Pattern PARTY_SOMEONE_JOINED =
            Pattern.compile(PARTY_PREFIX_REGEX + "(.+) has joined your party, say hello!");

    private static final Pattern PARTY_OTHER_LEFT = Pattern.compile(PARTY_PREFIX_REGEX + "(.+) has left the party!");
    private static final Pattern PARTY_OTHER_KICKED =
            Pattern.compile(PARTY_PREFIX_REGEX + "(.+) has been kicked from the party!");

    private static final Pattern PARTY_NEW_LEADER =
            Pattern.compile(PARTY_PREFIX_REGEX + "(?:§c)?(.+)§e is now the Party Leader!.*");

    private static final Pattern PARTY_RESTORED_SELF =
            Pattern.compile(PARTY_PREFIX_REGEX + "Your previous party was restored");

    private boolean expectingPartyMessage = false; // Whether the client is expecting a response from "/party list"
    private long lastPartyRequest = 0; // The last time the client requested party data

    private boolean inParty; // Whether the player is in a party
    private String partyLeader = null; // The name of the party leader
    private List<String> partyMembers = new ArrayList<>(); // All party members

    public PartyModel() {
        resetData();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.WORLD) {
            requestData();
        } else {
            resetData();
        }
    }

    @SubscribeEvent
    public void onChatReceived(ChatMessageEvent.Match event) {
        StyledText chatMessage = StyledTextUtils.unwrap(event.getMessage()).stripAlignment();

        if (tryParsePartyMessages(chatMessage)) return;

        if (expectingPartyMessage) {
            if (tryParseNoPartyMessage(chatMessage) || tryParsePartyList(chatMessage)) {
                event.cancelChat();
                expectingPartyMessage = false;
            }
        }
    }

    private boolean tryParsePartyMessages(StyledText styledText) {
        if (styledText.matches(PARTY_PLAYER_CREATED)) {
            WynntilsMod.info("Player created a new party.");
            inParty = true;
            partyLeader = McUtils.playerName();
            partyMembers = new ArrayList<>(List.of(partyLeader));
            return true;
        }

        if (styledText.matches(PARTY_PLAYER_LEFT)
                || styledText.matches(PARTY_PLAYER_DISBANDED)
                || styledText.matches(PARTY_PLAYER_KICKED)) {
            WynntilsMod.info("Player is no longer in a party.");
            resetData();
            return true;
        }

        Matcher matcher = styledText.getMatcher(PARTY_SOMEONE_JOINED);
        if (matcher.matches()) {
            String player = matcher.group(1);
            if (player.equals(McUtils.playerName())) {
                WynntilsMod.info("Player joined a new party, requesting party list.");
                requestData();
            } else {
                WynntilsMod.info("Player's party has a new member: " + player);
                partyMembers.add(player);
            }
            return true;
        }

        matcher = styledText.getMatcher(PARTY_OTHER_LEFT);
        if (matcher.matches()) {
            partyMembers.remove(matcher.group(1));
            return true;
        }

        matcher = styledText.getMatcher(PARTY_OTHER_KICKED);
        if (matcher.matches()) {
            partyMembers.remove(matcher.group(1));
            return true;
        }

        matcher = styledText.getMatcher(PARTY_NEW_LEADER);
        if (matcher.matches()) {
            partyLeader = matcher.group(1);
            return true;
        }

        if (styledText.matches(PARTY_RESTORED_SELF)) {
            requestData();
            return true;
        }

        return false;
    }

    private boolean tryParseNoPartyMessage(StyledText styledText) {
        if (styledText.matches(PARTY_COMMAND_FAILED)) {
            resetData();
            WynntilsMod.info("Player is not in a party.");
            return true;
        }
        return false;
    }

    private boolean tryParsePartyList(StyledText styledText) {
        Matcher matcher = styledText.getMatcher(PARTY_LIST_ALL);
        if (!matcher.matches()) return false;

        String[] partyList = StyledText.fromString(matcher.group(1))
                .getStringWithoutFormatting()
                .split("(?:,(?: and)? )");
        List<String> newPartyMembers = new ArrayList<>();
        Collections.addAll(newPartyMembers, partyList);

        // Attempt to look for party leader with pattern.
        // If fail, assume we are leader (no special color will appear in list).
        Matcher leaderMatcher = styledText.getMatcher(PARTY_LIST_LEADER);
        partyLeader = leaderMatcher.find() ? leaderMatcher.group(1) : McUtils.playerName();

        // Sort by the order they appear in the old list, to preserve order.
        partyMembers = newPartyMembers.stream()
                .sorted(Comparator.comparing(element -> partyMembers.indexOf(element)))
                .collect(Collectors.toList());

        inParty = true;
        WynntilsMod.info("Successfully updated party list, user has " + partyList.length + " party members.");
        return true;
    }

    private void resetData() {
        partyMembers = new ArrayList<>();
        partyLeader = null;
        inParty = false;
    }

    /**
     * Sends "/party list" to Wynncraft and waits for the response.
     * Skips if the last request was less than 250ms ago.
     */
    public void requestData() {
        if (McUtils.player() == null) return;

        if (System.currentTimeMillis() - lastPartyRequest < 250) {
            WynntilsMod.info("Skipping party list request because it was requested less than 250ms ago.");
            return;
        }

        expectingPartyMessage = true;
        lastPartyRequest = System.currentTimeMillis();
        Handlers.Command.queueCommand("party list");
    }

    public boolean isInParty() {
        return inParty;
    }

    public boolean isPartyLeader(String userName) {
        return userName.equals(partyLeader);
    }

    public Optional<String> getPartyLeader() {
        return Optional.ofNullable(partyLeader);
    }

    public List<String> getPartyMembers() {
        return partyMembers;
    }
}

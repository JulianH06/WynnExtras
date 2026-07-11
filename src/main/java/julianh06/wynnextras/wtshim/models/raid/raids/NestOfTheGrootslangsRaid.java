// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.raids;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import java.util.Map;
import java.util.TreeMap;

public class NestOfTheGrootslangsRaid extends RaidKind {
    private static final String RAID_NAME = "Nest of the Grootslangs";
    private static final String ABBREVIATION = "NOG";
    private static final CustomColor RAID_COLOR = CustomColor.fromHexString("#00aa00ff");
    private static final StyledText ENTRY_TITLE = StyledText.fromString("§2Nest of The Grootslangs");

    public NestOfTheGrootslangsRaid() {
        super(RAID_NAME, ABBREVIATION, RAID_COLOR, ENTRY_TITLE, buildRoomMap(), buildMajorIdMap());
    }

    private static Map<Integer, Map<String, String>> buildRoomMap() {
        Map<Integer, Map<String, String>> nameMap = new TreeMap<>();

        Map<String, String> challenge1Map = Map.of(
                "Hold the platform", "Slimey Platform",
                "Hold and defend", "Tower Defense");
        Map<String, String> challenge2Map = Map.of("Collect 10 Slimy Goo", "Slime Gathering");
        Map<String, String> challenge3Map = Map.of(
                "Have a player pick up", "Tunnel Traversal",
                "2 players must", "Minibosses");

        nameMap.put(1, challenge1Map);
        nameMap.put(2, challenge2Map);
        nameMap.put(3, challenge3Map);
        nameMap.put(4, Map.of("Slay the Restless", "Grootslang Wyrmling"));

        return nameMap;
    }

    private static Map<String, Map<Integer, String>> buildMajorIdMap() {
        return Map.of(
                "Berserk", Map.of(1, "Explosive Impact"),
                "Lightbearer", Map.of(3, "Transcendence"),
                "Pestilent", Map.of(3, "Plague"),
                "Bedrock", Map.of(3, "Altruism"));
    }
}

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

public class TheWartornPalaceRaid extends RaidKind {
    private static final String RAID_NAME = "The Wartorn Palace";
    private static final String ABBREVIATION = "WTP";
    private static final CustomColor RAID_COLOR = CustomColor.fromHexString("#FF5555FF");
    private static final StyledText ENTRY_TITLE = StyledText.fromString("§#00F010FFThe Wartorn Palace");

    public TheWartornPalaceRaid() {
        super(RAID_NAME, ABBREVIATION, RAID_COLOR, ENTRY_TITLE, buildRoomMap(), buildMajorIdMap());
    }

    private static Map<Integer, Map<String, String>> buildRoomMap() {
        Map<Integer, Map<String, String>> nameMap = new TreeMap<>();

        Map<String, String> challenge1Map = Map.of(
                "Fight through the", "Grand Aisles",
                "Collect the sonic", "Regal Ballroom");
        Map<String, String> challenge2Map = Map.of("Slay the Knightmare", "Statuary Hall");
        Map<String, String> challenge3Map = Map.of("Rip out the artifact", "The Spire's Shadow");

        nameMap.put(1, challenge1Map);
        nameMap.put(2, challenge2Map);
        nameMap.put(3, challenge3Map);
        nameMap.put(4, Map.of("Unknown", "Anathema")); // TODO: When this is fixed on Wynn, change this

        return nameMap;
    }

    private static Map<String, Map<Integer, String>> buildMajorIdMap() {
        return Map.of(
                "Opulent", Map.of(2, "Transcendence"),
                "Ingenious", Map.of(2, "Phoenix-Born"));
    }
}

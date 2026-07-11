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

public class TheCanyonColossusRaid extends RaidKind {
    private static final String RAID_NAME = "The Canyon Colossus";
    private static final String ABBREVIATION = "TCC";
    private static final CustomColor RAID_COLOR = CustomColor.fromHexString("#00aaaaff");
    private static final StyledText ENTRY_TITLE = StyledText.fromString("§#5f968bffThe Canyon Colossus");

    public TheCanyonColossusRaid() {
        super(RAID_NAME, ABBREVIATION, RAID_COLOR, ENTRY_TITLE, buildRoomMap(), buildMajorIdMap());
    }

    private static Map<Integer, Map<String, String>> buildRoomMap() {
        Map<Integer, Map<String, String>> nameMap = new TreeMap<>();

        Map<String, String> challenge1Map = Map.of(
                "Hold the Upper and", "2 Platforms",
                "Use water on", "Lava Lake");
        Map<String, String> challenge2Map = Map.of(
                "Find and reach the", "Labyrinth",
                "Wake the ancient", "Golem Escort");
        Map<String, String> challenge3Map = Map.of("Activate 4 Binding", "Binding Seal");

        nameMap.put(1, challenge1Map);
        nameMap.put(2, challenge2Map);
        nameMap.put(3, challenge3Map);
        nameMap.put(4, Map.of("Calm the canyon's", "The Canyon Colossus"));

        return nameMap;
    }

    private static Map<String, Map<Integer, String>> buildMajorIdMap() {
        return Map.of(
                "Intrepid",
                Map.of(
                        1, "Greed",
                        2, "Altruism",
                        3, "Guardian"),
                "Stonewalker",
                Map.of(3, "Explosive Impact"));
    }
}

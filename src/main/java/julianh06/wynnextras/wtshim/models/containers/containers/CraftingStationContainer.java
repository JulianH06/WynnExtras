// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - CraftingStationContainer. Title prefix + per-profession glyph, verbatim from Wynntils.
 * DEVIATION: Wynntils' HighlightableProfessionProperty/ContainerBounds dropped (unported). */
package julianh06.wynnextras.wtshim.models.containers.containers;

import julianh06.wynnextras.wtshim.models.containers.Container;
import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import java.util.regex.Pattern;

public class CraftingStationContainer extends Container {
    private final ProfessionType professionType;

    private static final String CRAFTING_STATION_TITLE_PREFIX = "\uDAFF\uDFF8\uE053\uDAFF\uDF80";

    public CraftingStationContainer(ProfessionType professionType) {
        super(Pattern.compile(CRAFTING_STATION_TITLE_PREFIX + getContainerTitleGlyph(professionType)));
        this.professionType = professionType;
    }

    private static String getContainerTitleGlyph(ProfessionType professionType) {
        return switch (professionType) {
            case ALCHEMISM -> "\uF041";
            case ARMOURING -> "\uF042";
            case COOKING -> "\uF043";
            case JEWELING -> "\uF044";
            case SCRIBING -> "\uF045";
            case TAILORING -> "\uF046";
            case WEAPONSMITHING -> "\uF047";
            case WOODWORKING -> "\uF048";
            default -> " ";
        };
    }

    public ProfessionType getProfessionType() {
        return professionType;
    }

    @Override
    public String getContainerName() {
        return professionType.getDisplayName() + "Station";
    }
}

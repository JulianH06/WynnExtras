// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - CharacterInfoContainer. Title pattern verbatim from Wynntils. */
package julianh06.wynnextras.wtshim.models.containers.containers;

import julianh06.wynnextras.wtshim.models.containers.Container;
import java.util.regex.Pattern;

public class CharacterInfoContainer extends Container {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFDC\uE003");

    public CharacterInfoContainer() {
        super(TITLE_PATTERN);
    }
}

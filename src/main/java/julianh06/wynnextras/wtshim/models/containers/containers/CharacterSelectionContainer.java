// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - CharacterSelectionContainer. Title pattern verbatim from Wynntils. */
package julianh06.wynnextras.wtshim.models.containers.containers;

import julianh06.wynnextras.wtshim.models.containers.Container;
import java.util.regex.Pattern;

public class CharacterSelectionContainer extends Container {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFD5\uE01F");

    public CharacterSelectionContainer() {
        super(TITLE_PATTERN);
    }
}

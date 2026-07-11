// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - ItemIdentifierContainer. Title pattern verbatim from Wynntils. */
package julianh06.wynnextras.wtshim.models.containers.containers;

import julianh06.wynnextras.wtshim.models.containers.Container;
import java.util.regex.Pattern;

public class ItemIdentifierContainer extends Container {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFF8\uE018");

    public ItemIdentifierContainer() {
        super(TITLE_PATTERN);
    }
}

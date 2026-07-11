// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.bossbar;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.handlers.bossbar.TrackedBar;
import java.util.regex.Pattern;

public class ParasiteOvertakenBar extends TrackedBar {
    // Test in ParasiteOvertakenBar_OVERTAKEN_PATTERN
    private static final Pattern OVERTAKEN_PATTERN =
            Pattern.compile("(§#aa00ffff)?.*(§8.*)?§r\uDAFF\uDF81§fOVERTAKEN\uDB00\uDC49");

    public ParasiteOvertakenBar() {
        super(OVERTAKEN_PATTERN);
    }

    @Override
    protected void reset() {
        super.reset();

        Models.Raid.resetParasiteOvertaken();
    }
}

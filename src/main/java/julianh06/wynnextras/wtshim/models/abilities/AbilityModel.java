// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * SLIM: only the bloodPoolBar is ported (the sole bar WynnExtras reads, via
 * BloodSorrowTimer). Wynntils' other ability bars (awakened/corrupted/focus/…) and
 * its ability-tree parsing are intentionally NOT ported — no WynnExtras caller uses
 * them. bloodPoolBar is a real TrackedBar registered on Handlers.BossBar; the boss-bar
 * handler (phase 2) feeds it name/progress packets in-game.
 */
package julianh06.wynnextras.wtshim.models.abilities;

import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.components.Model;
import julianh06.wynnextras.wtshim.handlers.bossbar.TrackedBar;
import julianh06.wynnextras.wtshim.models.abilities.bossbars.BloodPoolBar;

public final class AbilityModel extends Model {
    public static final TrackedBar bloodPoolBar = new BloodPoolBar();

    public AbilityModel() {
        Handlers.BossBar.registerBar(bloodPoolBar);
    }
}

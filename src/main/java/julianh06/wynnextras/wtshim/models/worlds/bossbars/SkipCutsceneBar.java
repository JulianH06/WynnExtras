// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.models.worlds.bossbars;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.handlers.bossbar.TrackedBar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkipCutsceneBar extends TrackedBar {
    private static final Pattern CUTSCENE_SKIP_PATTERN =
            Pattern.compile("§7Press§r §f Swap Hands §7to skip( §8- §f\\d+§7/§f\\d+)?");

    public SkipCutsceneBar() {
        super(CUTSCENE_SKIP_PATTERN);
    }

    @Override
    public void onUpdateName(Matcher match) {
        boolean groupCutscene = match.group(1) != null;
        Models.WorldState.cutsceneStarted(groupCutscene);
    }

    @Override
    protected void reset() {
        super.reset();

        Models.WorldState.cutsceneEnded();
    }
}

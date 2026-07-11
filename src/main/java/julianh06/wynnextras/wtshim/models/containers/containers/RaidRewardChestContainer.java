// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Relocated for the WynnExtras standalone compat shim (wtshim).
 *
 * DEVIATION: getAspectBounds()/ContainerBounds dropped — RaidModel does not use them and
 * ContainerBounds is Phase-5 surface. The shim's ContainerModel never instantiates this class
 * (title-pattern recognition is Phase 5), so RaidModel's reward-chest handlers are currently
 * inert; this port exists so RaidModel compiles verbatim.
 */
package julianh06.wynnextras.wtshim.models.containers.containers;

import julianh06.wynnextras.wtshim.models.containers.Container;
import java.util.regex.Pattern;

/**
 * This represents the container for end raid rewards.
 */
public class RaidRewardChestContainer extends Container {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFEA\uE00E");

    public static final Pattern REROLL_CONFIRM_PATTERN = Pattern.compile("§7Click again to confirm");
    public static final int REROLL_REWARDS_SLOT = 5;

    public RaidRewardChestContainer() {
        super(TITLE_PATTERN);
    }
}

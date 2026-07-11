// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.mc.event;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import net.neoforged.bus.api.Event;

/** Fires on change to footer of scoreboard */
public class PlayerInfoFooterChangedEvent extends Event {
    private final StyledText footer;

    public StyledText getFooter() {
        return footer;
    }

    public PlayerInfoFooterChangedEvent(StyledText footer) {
        this.footer = footer;
    }
}

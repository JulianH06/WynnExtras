// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * SIMPLIFIED: The upstream event carries a List<ActionBarSegment> produced by
 * Wynntils' ActionBarHandler, which is not part of this shim. It is reduced here to
 * carry the raw action bar text (the already-extracted data consumers need).
 */
package julianh06.wynnextras.wtshim.mc.event;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import net.neoforged.bus.api.Event;

/**
 * Fired when the action bar is updated.
 */
public class ActionBarUpdatedEvent extends Event {
    private final StyledText content;

    public ActionBarUpdatedEvent(StyledText content) {
        this.content = content;
    }

    public StyledText getContent() {
        return content;
    }
}

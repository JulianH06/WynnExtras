// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings
 * (net.minecraft.network.chat.Component -> net.minecraft.text.Text).
 */
package julianh06.wynnextras.wtshim.mc.event;

import java.util.UUID;
import net.minecraft.text.Text;
import net.neoforged.bus.api.Event;

/** Fires for changes in player info in the tab list. */
public abstract class PlayerInfoEvent extends Event {
    private final UUID id;

    protected PlayerInfoEvent(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    /** Fires on change to a tab-list entry's display name. */
    public static class PlayerDisplayNameChangeEvent extends PlayerInfoEvent {
        private final Text displayName;

        public PlayerDisplayNameChangeEvent(UUID id, Text displayName) {
            super(id);
            this.displayName = displayName;
        }

        public Text getDisplayName() {
            return displayName;
        }
    }

    /** Fires on addition of a tab-list entry. */
    public static class PlayerLogInEvent extends PlayerInfoEvent {
        private final String name;

        public PlayerLogInEvent(UUID id, String name) {
            super(id);
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /** Fires on removal of a tab-list entry. */
    public static class PlayerLogOutEvent extends PlayerInfoEvent {
        public PlayerLogOutEvent(UUID id) {
            super(id);
        }
    }
}

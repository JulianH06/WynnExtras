package julianh06.wynnextras.event;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.api.WEEvent;

public class CharInputEvent extends WEEvent {
    private final char character;

    public CharInputEvent(char character) {
        this.character = character;
    }

    public char getCharacter() {
        return character;
    }

    public static boolean initialized = false;
    public static void init() {
        WynnExtras.LOGGER.info("Initialized WynnExtras CharInputEvent");
        initialized = true;
    }
}

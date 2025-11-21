package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;

public class CharInputEvent extends WEEvent {
    private final char character;

    public CharInputEvent(char character) {
        this.character = character;
    }

    public char getCharacter() {
        return character;
    }

    // Falls du ein init‐Flag oder Registrierung brauchst:
    public static boolean initialized = false;
    public static void init() {
        System.out.println("Initialized WynnExtras CharInputEvent");
        initialized = true;
    }
}

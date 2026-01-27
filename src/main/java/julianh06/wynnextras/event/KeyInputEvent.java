package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;

public class KeyInputEvent extends WEEvent {
    public static boolean initialized = false;
    private final int action;
    private final int key;
    private final int modifiers;
    private final int scanCode;

    public KeyInputEvent(int key, int scanCode, int action, int modifiers) {
        this.action = action;
        this.key = key;
        this.modifiers = modifiers;
        this.scanCode = scanCode;
    }

    public int getAction() {
        return this.action;
    }

    public int getKey() {
        return this.key;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public int getScanCode() {
        return this.scanCode;
    }

    public static void init() {
        System.out.println("Initialized WynnExtras KeyInputEvent");
        initialized = true;
    }
}

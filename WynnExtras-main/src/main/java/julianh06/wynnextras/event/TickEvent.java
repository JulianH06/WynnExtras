package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;

public final class TickEvent extends WEEvent {
    public int ticks;

    public TickEvent(int ticks) {
        this.ticks = ticks;
    }
}

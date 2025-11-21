package julianh06.wynnextras.event.api;

import net.neoforged.bus.api.Event;

public class WEEvent extends Event {
    public boolean post() {
        return WEEventBus.postEvent(this);
    }
}

package julianh06.wynnextras.event.api;

import julianh06.wynnextras.mixin.Accessor.EventAccessor;
import net.neoforged.bus.api.Event;

public class WEEvent extends Event {
    public boolean post() {
        return WEEventBus.postEvent(this);
    }

    public void setCanceled(boolean canceled) {
        ((EventAccessor) this).setCanceled(canceled);
    }
}

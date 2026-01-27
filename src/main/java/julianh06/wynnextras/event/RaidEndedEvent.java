package julianh06.wynnextras.event;

import com.wynntils.models.raid.type.RaidInfo;
import julianh06.wynnextras.event.api.WEEvent;

public class RaidEndedEvent extends WEEvent {
    private final RaidInfo raidInfo;

    public RaidEndedEvent(RaidInfo raidInfo) {
        this.raidInfo = raidInfo;
    }

    public RaidInfo getRaid() {
        return this.raidInfo;
    }

    public static class Failed extends RaidEndedEvent {
        public Failed(RaidInfo raidInfo) {
            super(raidInfo);
        }
    }

    public static class Completed extends RaidEndedEvent {
        public Completed(RaidInfo raidInfo) {
            super(raidInfo);
        }
    }
}

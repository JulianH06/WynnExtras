package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import julianh06.wynnextras.features.raid.RaidSnapshot;

public class RaidEndedEvent extends WEEvent {
    private final RaidSnapshot raidInfo;

    public RaidEndedEvent(RaidSnapshot raidInfo) {
        this.raidInfo = raidInfo;
    }

    public RaidSnapshot getRaid() {
        return this.raidInfo;
    }

    public static class Failed extends RaidEndedEvent {
        public Failed(RaidSnapshot raidInfo) {
            super(raidInfo);
        }
    }

    public static class Completed extends RaidEndedEvent {
        public Completed(RaidSnapshot raidInfo) {
            super(raidInfo);
        }
    }
}

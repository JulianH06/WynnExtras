package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.core.WynnExtras;
import com.wynntils.models.raid.type.RaidInfo;
import julianh06.wynnextras.mixin.Invoker.RaidInfoInvoker;

import java.util.ArrayList;
import java.util.List;

public class RaidData {
    public RaidInfo raidInfo;
    public List<String> players = new ArrayList<>();
    public long raidEndTime;
    public long raidStartTime;
    public long duration;
    public boolean completed;

    public RaidData(RaidInfo raidInfo, List<String> players, long raidEndTime, boolean completed) {
        this.raidInfo = raidInfo;
        this.players = players;
        this.raidEndTime = raidEndTime;
        this.completed = completed;

        // Calculate start time from end time minus duration
        // Use the raidStartTime from raidInfo if available, otherwise calculate it
        long startTimeFromEvent = raidInfo.getRaidStartTime();
        if (startTimeFromEvent > 0) {
            this.raidStartTime = startTimeFromEvent;
        } else {
            // Fallback: calculate from end time
            this.raidStartTime = raidEndTime - raidInfo.getTimeInRaid();
        }

        this.duration = ((RaidInfoInvoker) raidInfo).invokeGetTimeInRooms();

        // Debug logging
        WynnExtras.LOGGER.info("[WynnExtras] RaidData created:");
        WynnExtras.LOGGER.info("  Start time: " + this.raidStartTime);
        WynnExtras.LOGGER.info("  End time: " + this.raidEndTime);
        WynnExtras.LOGGER.info("  Duration: " + this.duration);
    }
}

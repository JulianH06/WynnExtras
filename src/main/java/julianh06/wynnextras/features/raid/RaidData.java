package julianh06.wynnextras.features.raid;

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
        this.raidStartTime = System.currentTimeMillis() - raidInfo.getTimeInRaid();
        this.duration = ((RaidInfoInvoker) raidInfo).invokeGetTimeInRooms();
    }
}

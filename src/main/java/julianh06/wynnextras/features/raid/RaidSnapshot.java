package julianh06.wynnextras.features.raid;

import java.util.Map;

public final class RaidSnapshot {
    private final WERaidKind raidKind;
    private final Map<Integer, RaidRoomData> challenges;
    private final long raidStartTime;
    private final long timeInRaid;
    private final long timeInRooms;

    public RaidSnapshot(WERaidKind raidKind, Map<Integer, RaidRoomData> challenges,
                        long raidStartTime, long timeInRaid, long timeInRooms) {
        this.raidKind = raidKind == null ? WERaidKind.UNKNOWN : raidKind;
        this.challenges = challenges == null ? Map.of() : Map.copyOf(challenges);
        this.raidStartTime = raidStartTime;
        this.timeInRaid = timeInRaid;
        this.timeInRooms = timeInRooms;
    }

    public WERaidKind raidKind() { return raidKind; }
    public Map<Integer, RaidRoomData> challenges() { return challenges; }
    public long raidStartTime() { return raidStartTime; }
    public long timeInRaid() { return timeInRaid; }
    public long timeInRooms() { return timeInRooms; }

}

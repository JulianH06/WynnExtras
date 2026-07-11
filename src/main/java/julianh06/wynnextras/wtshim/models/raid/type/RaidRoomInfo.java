// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.type;

public class RaidRoomInfo {
    private final String roomName;
    private final long roomStartTime;

    private long roomDamage = 0L;
    private long roomEndTime = -1L;
    private long roomTotalTime = -1L;

    public RaidRoomInfo(String roomName) {
        this.roomName = roomName;
        this.roomStartTime = System.currentTimeMillis();
    }

    public String getRoomName() {
        return roomName;
    }

    public long getRoomStartTime() {
        return roomStartTime;
    }

    public long getRoomEndTime() {
        return roomEndTime;
    }

    public void setRoomEndTime(long roomEndTime) {
        this.roomEndTime = roomEndTime;

        setRoomTotalTime();
    }

    public long getRoomTotalTime() {
        if (roomTotalTime != -1L) return roomTotalTime;

        return System.currentTimeMillis() - roomStartTime;
    }

    public void addDamage(long newDamage) {
        roomDamage += newDamage;
    }

    public long getRoomDamage() {
        return roomDamage;
    }

    private void setRoomTotalTime() {
        roomTotalTime = roomEndTime - roomStartTime;
    }
}

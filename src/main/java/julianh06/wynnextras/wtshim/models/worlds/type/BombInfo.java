// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — BombInfo record.
 * Fields match Wynntils' shape: (user, bombType, server, startTimeMillis, activeMinutes).
 */
package julianh06.wynnextras.wtshim.models.worlds.type;

public record BombInfo(String user, BombType bomb, String server, long startTime, float activeMinutes) {
    public String getUser() { return user; }
    public BombType getBomb() { return bomb; }
    public String getServer() { return server; }
    public long getStartTime() { return startTime; }

    public long getDurationMillis() {
        return (long) (activeMinutes * 60_000f);
    }

    public long getRemainingMillis() {
        long ending = startTime + getDurationMillis();
        return Math.max(0L, ending - System.currentTimeMillis());
    }

    public boolean isActive() { return getRemainingMillis() > 0; }

    public long getRemainingLong() { return getRemainingMillis(); }

    public String getRemainingString() {
        long ms = getRemainingMillis();
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m % 60, s % 60);
        if (m > 0) return String.format("%dm %02ds", m, s % 60);
        return s + "s";
    }
}

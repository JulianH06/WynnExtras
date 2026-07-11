// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — Time record (milliseconds wall-clock). */
package julianh06.wynnextras.wtshim.utils.type;

public record Time(long timestamp) {
    public static Time now() { return new Time(System.currentTimeMillis()); }
    public boolean after(Time other) { return this.timestamp > other.timestamp; }
    public boolean before(Time other) { return this.timestamp < other.timestamp; }
    public long asMillis() { return timestamp; }
}

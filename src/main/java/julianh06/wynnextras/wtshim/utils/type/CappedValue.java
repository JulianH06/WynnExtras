// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — CappedValue. */
package julianh06.wynnextras.wtshim.utils.type;

public record CappedValue(int current, int max) {
    public static final CappedValue EMPTY = new CappedValue(0, 0);

    public float getProgress() { return max == 0 ? 0f : (float) current / max; }

    public double getPercentage() { return max == 0 ? 0.0 : (current * 100.0) / max; }

    public int getPercentageInt() { return Math.round((float) getPercentage()); }
}

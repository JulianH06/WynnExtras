// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of RangedValue.
 */
package julianh06.wynnextras.wtshim.utils.type;

public record RangedValue(int low, int high) {
    public static final RangedValue NONE = new RangedValue(0, 0);

    public static RangedValue of(int low, int high) {
        return new RangedValue(low, high);
    }

    public boolean inRange(int value) {
        return value >= low && value <= high;
    }

    public int average() {
        return (low + high) / 2;
    }

    public boolean isFixed() {
        return low == high;
    }
}

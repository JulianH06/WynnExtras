// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of the Pair record.
 */
package julianh06.wynnextras.wtshim.utils.type;

public record Pair<A, B>(A a, B b) {
    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    public A key() { return a; }
    public B value() { return b; }
}

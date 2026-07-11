// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — Config stub (a field-level config wrapper). */
package julianh06.wynnextras.wtshim.core.persisted.config;

public class Config<T> {
    private T value;
    public Config(T def) { this.value = def; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }
}

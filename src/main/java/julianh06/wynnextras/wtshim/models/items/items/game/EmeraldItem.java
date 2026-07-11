// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Adapted for the WynnExtras standalone compat shim (wtshim).
 *
 * The shim flattens the item hierarchy (all game items extend WynnItem directly), so this drops
 * the GameItem base and the EmeraldValuedItemProperty interface which the shim does not yet have.
 * TODO(phase6-item-pipeline): the heuristic shim ItemModel does not produce EmeraldItem yet, so
 * RaidModel's emerald-in-reward-slot check is inert until the item pipeline lands.
 */
package julianh06.wynnextras.wtshim.models.items.items.game;

import julianh06.wynnextras.wtshim.models.emeralds.type.EmeraldUnits;
import julianh06.wynnextras.wtshim.models.items.WynnItem;
import java.util.function.Supplier;

public class EmeraldItem extends WynnItem {
    private final Supplier<Integer> amountSupplier;
    private final EmeraldUnits unit;

    public EmeraldItem(Supplier<Integer> amountSupplier, EmeraldUnits unit) {
        this.amountSupplier = amountSupplier;
        this.unit = unit;
    }

    public int getAmount() {
        return amountSupplier.get();
    }

    public EmeraldUnits getUnit() {
        return unit;
    }

    public int getEmeraldValue() {
        return getAmount() * unit.getMultiplier();
    }

    @Override
    public String toString() {
        return "EmeraldItem{" + "amount=" + getAmount() + ", unit=" + unit + '}';
    }
}

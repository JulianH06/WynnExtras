// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemMaterial (SLIM graft).
 * Wynntils' ItemMaterial has a suite of static factory helpers (fromGearType, fromItemId, player
 * heads, etc.) that reach into Models.WynnItem/SkinUtils/Mojmap data-fixers. The fork only ever
 * does `new ItemMaterial(ItemStack.EMPTY)` and never reads the stack back, so the shim keeps only
 * the record shape (Yarn ItemStack). Add factories here if a future caller needs them.
 */
package julianh06.wynnextras.wtshim.models.wynnitem.type;

import net.minecraft.item.ItemStack;

public record ItemMaterial(ItemStack itemStack) {}

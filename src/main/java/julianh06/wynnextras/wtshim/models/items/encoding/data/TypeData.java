// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.data;

import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemType;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;

public record TypeData(ItemType itemType) implements ItemData {
    public static TypeData fromByte(UnsignedByte versionByte) {
        return new TypeData(ItemType.fromEncodingId(versionByte.toByte()));
    }
}

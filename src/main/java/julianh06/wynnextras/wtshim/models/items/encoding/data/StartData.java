// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.data;

import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;

public record StartData(ItemTransformingVersion version) implements ItemData {
    public static StartData fromByte(UnsignedByte versionByte) {
        return new StartData(ItemTransformingVersion.fromId(versionByte.toByte()));
    }
}

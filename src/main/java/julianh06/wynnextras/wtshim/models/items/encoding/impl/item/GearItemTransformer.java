// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GearItemTransformer (encode-only, identity encoding).
 *
 * Encodes a GearItem into a NameData block only. Wynntils additionally encodes
 * IdentificationData / PowderData / RerollData / ShinyData, but those require data the slim shim
 * item pipeline does not carry:
 *   - IdentificationData needs per-stat numerical IDs (Models.Stat / id_keys.json) and per-stat
 *     internal rolls. The shim has no Models.Stat, its StatType.apiName is a heuristic camelCase
 *     guess (not the official Wynncraft key), and StatActualValue carries no internal roll.
 *   - PowderData / RerollData / ShinyData are not parsed from lore by the shim at all.
 * Emitting fabricated IDs/rolls would decode to garbage in Wynntils, so we omit those blocks.
 *
 * The resulting Start+Type+Name+End byte sequence is a valid Wynntils gear encoding that decodes
 * as the named base item (without roll/powder info). This is byte-format-faithful for what it emits.
 *
 * TODO(phase-stat-hierarchy): to encode identifications faithfully, port Models.Stat + id_keys.json
 * + the official StatType registry + internal-roll reconstruction (the subsystem Phase 6b declined).
 */
package julianh06.wynnextras.wtshim.models.items.encoding.impl.item;

import julianh06.wynnextras.wtshim.models.items.encoding.data.NameData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.EncodingSettings;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemType;
import julianh06.wynnextras.wtshim.models.items.items.game.GearItem;
import java.util.ArrayList;
import java.util.List;

public class GearItemTransformer extends ItemTransformer<GearItem> {
    @Override
    protected List<ItemData> encodeItem(GearItem item, EncodingSettings encodingSettings) {
        List<ItemData> dataList = new ArrayList<>();
        dataList.add(NameData.of(item.getName()));
        return dataList;
    }

    @Override
    public ItemType getType() {
        return ItemType.GEAR;
    }
}

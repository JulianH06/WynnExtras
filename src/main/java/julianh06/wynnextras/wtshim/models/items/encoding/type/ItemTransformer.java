// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ItemTransformer (encode-only).
 *
 * Adapted from Wynntils: the decode side (decodeItem / processIdentifications) is intentionally
 * NOT ported. WynnExtras only ever encodes items to shareable strings (ItemUtils.itemStackToItemString);
 * decoding happens in real Wynntils / WynnBuilder on the receiving end. Porting decode would require
 * the full StatType registry + Models.Stat + StatCalculator that the shim deliberately does not carry
 * (see PORT_PROGRESS Phase 6b). The TypeData block prepend and encode() contract are byte-identical.
 */
package julianh06.wynnextras.wtshim.models.items.encoding.type;

import julianh06.wynnextras.wtshim.models.items.WynnItem;
import julianh06.wynnextras.wtshim.models.items.encoding.data.TypeData;
import java.util.ArrayList;
import java.util.List;

public abstract class ItemTransformer<T extends WynnItem> {
    public final List<ItemData> encode(T item, EncodingSettings encodingSettings) {
        List<ItemData> dataList = new ArrayList<>();
        dataList.add(new TypeData(getType()));
        dataList.addAll(encodeItem(item, encodingSettings));
        return List.copyOf(dataList);
    }

    protected abstract List<ItemData> encodeItem(T item, EncodingSettings encodingSettings);

    public abstract ItemType getType();
}

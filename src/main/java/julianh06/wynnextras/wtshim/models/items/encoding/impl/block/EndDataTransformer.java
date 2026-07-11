// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.impl.block;

import julianh06.wynnextras.wtshim.models.items.encoding.data.EndData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformerType;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.type.ArrayReader;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;

public class EndDataTransformer extends DataTransformer<EndData> {
    @Override
    public ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, EndData data) {
        // End data is always empty
        return ErrorOr.of(new UnsignedByte[0]);
    }

    @Override
    public ErrorOr<EndData> decodeData(ItemTransformingVersion version, ArrayReader<UnsignedByte> byteReader) {
        // End data is always empty
        return ErrorOr.of(new EndData());
    }

    @Override
    public byte getId() {
        return DataTransformerType.END_DATA_TRANSFORMER.getId();
    }
}

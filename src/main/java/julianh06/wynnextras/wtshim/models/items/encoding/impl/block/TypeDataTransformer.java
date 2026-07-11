// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.impl.block;

import julianh06.wynnextras.wtshim.models.items.encoding.data.TypeData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformerType;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.type.ArrayReader;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;

public class TypeDataTransformer extends DataTransformer<TypeData> {
    @Override
    public ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, TypeData data) {
        return switch (version) {
            case VERSION_1, VERSION_2 ->
                ErrorOr.of(new UnsignedByte[] {UnsignedByte.of(data.itemType().getEncodingId())});
        };
    }

    @Override
    public ErrorOr<TypeData> decodeData(ItemTransformingVersion version, ArrayReader<UnsignedByte> byteReader) {
        return switch (version) {
            case VERSION_1, VERSION_2 -> decodeType(byteReader);
        };
    }

    private static ErrorOr<TypeData> decodeType(ArrayReader<UnsignedByte> byteReader) {
        TypeData typeData = TypeData.fromByte(byteReader.read());
        if (typeData.itemType() == null) {
            return ErrorOr.error("Unknown item type.");
        }

        return ErrorOr.of(typeData);
    }

    @Override
    public byte getId() {
        return DataTransformerType.TYPE_DATA_TRANSFORMER.getId();
    }
}

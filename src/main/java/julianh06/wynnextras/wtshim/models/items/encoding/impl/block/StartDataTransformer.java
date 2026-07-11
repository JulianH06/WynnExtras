// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.impl.block;

import julianh06.wynnextras.wtshim.models.items.encoding.data.StartData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformerType;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.type.ArrayReader;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;

public class StartDataTransformer extends DataTransformer<StartData> {
    /**
     * Decodes the start data block. This is specially handled by the {@link ItemTransformerRegistry}.
     * @param byteReader The byte reader to read the data from.
     * @return The decoded start data.
     */
    public static ErrorOr<StartData> decodeData(ArrayReader<UnsignedByte> byteReader) {
        UnsignedByte idByte = byteReader.read();
        if (idByte.value() != DataTransformerType.START_DATA_TRANSFORMER.getId()) {
            return ErrorOr.error("Encoded data does not start with a start data block.");
        }

        UnsignedByte versionByte = byteReader.read();

        StartData startData = StartData.fromByte(versionByte);
        if (startData.version() == null) {
            return ErrorOr.error("Unknown version: " + versionByte);
        }

        return ErrorOr.of(startData);
    }

    @Override
    public ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, StartData data) {
        return switch (version) {
            case VERSION_1, VERSION_2 ->
                ErrorOr.of(new UnsignedByte[] {
                    UnsignedByte.of(data.version().getId()),
                });
        };
    }

    @Override
    public ErrorOr<StartData> decodeData(ItemTransformingVersion version, ArrayReader<UnsignedByte> byteReader) {
        // NOOP, should never be called
        throw new IllegalStateException("StartDataTransformer should never be called to decode data");
    }

    @Override
    public byte getId() {
        return DataTransformerType.START_DATA_TRANSFORMER.getId();
    }
}

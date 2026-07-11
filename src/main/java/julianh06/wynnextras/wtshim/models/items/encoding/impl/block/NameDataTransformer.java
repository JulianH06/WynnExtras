// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.models.items.encoding.impl.block;

import julianh06.wynnextras.wtshim.models.items.encoding.data.NameData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformerType;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.UnsignedByteUtils;
import julianh06.wynnextras.wtshim.utils.type.ArrayReader;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;
import java.util.ArrayList;
import java.util.List;

public class NameDataTransformer extends DataTransformer<NameData> {
    @Override
    public ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, NameData data) {
        return switch (version) {
            case VERSION_1, VERSION_2 -> encodeName(data.name().orElse(""));
        };
    }

    @Override
    public ErrorOr<NameData> decodeData(ItemTransformingVersion version, ArrayReader<UnsignedByte> byteReader) {
        return switch (version) {
            case VERSION_1, VERSION_2 -> decodeName(byteReader);
        };
    }

    private ErrorOr<UnsignedByte[]> encodeName(String name) {
        try {
            return ErrorOr.of(UnsignedByteUtils.encodeString(name));
        } catch (IllegalArgumentException e) {
            return ErrorOr.error("Name contains non-ASCII characters");
        }
    }

    private ErrorOr<NameData> decodeName(ArrayReader<UnsignedByte> byteReader) {
        List<UnsignedByte> bytes = new ArrayList<>();

        // Read until we find a null byte
        do {
            bytes.add(byteReader.read());
        } while (byteReader.hasRemaining() && byteReader.peek().value() != 0);

        // If this is the case, the byte reader ended, and we didn't find a null byte
        UnsignedByte nullByte = byteReader.read();
        if (nullByte.value() != 0) {
            return ErrorOr.error("Name data is not null terminated");
        }

        return ErrorOr.of(NameData.sanitized(UnsignedByteUtils.decodeString(bytes)));
    }

    @Override
    public byte getId() {
        return DataTransformerType.NAME_DATA_TRANSFORMER.getId();
    }
}

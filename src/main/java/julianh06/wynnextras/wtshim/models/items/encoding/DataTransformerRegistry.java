// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — DataTransformerRegistry.
 *
 * Byte-format-faithful port. Only the data blocks the shim can faithfully produce are registered:
 * Start / Type / Name / End. Wynntils registers 13 more (Identification, Powder, Reroll, Shiny,
 * damage/defense/requirements/durability, crafted-custom blocks, ...); those need stat-ID / roll /
 * element data the slim shim item pipeline does not carry (see GearItemTransformer). The encode/decode
 * machinery and the byte layout are unchanged, so any block that IS registered is byte-identical.
 */
package julianh06.wynnextras.wtshim.models.items.encoding;

import julianh06.wynnextras.wtshim.models.items.encoding.data.EndData;
import julianh06.wynnextras.wtshim.models.items.encoding.data.NameData;
import julianh06.wynnextras.wtshim.models.items.encoding.data.StartData;
import julianh06.wynnextras.wtshim.models.items.encoding.data.TypeData;
import julianh06.wynnextras.wtshim.models.items.encoding.impl.block.EndDataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.impl.block.NameDataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.impl.block.StartDataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.impl.block.TypeDataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.DataTransformer;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemData;
import julianh06.wynnextras.wtshim.models.items.encoding.type.ItemTransformingVersion;
import julianh06.wynnextras.wtshim.utils.EncodedByteBuffer;
import julianh06.wynnextras.wtshim.utils.type.ArrayReader;
import julianh06.wynnextras.wtshim.utils.type.ErrorOr;
import julianh06.wynnextras.wtshim.utils.type.UnsignedByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers and stores all data transformers. Data transformers transform between {@link ItemData}
 * and {@link UnsignedByte} arrays.
 */
public final class DataTransformerRegistry {
    private final DataTransformerMap dataTransformers = new DataTransformerMap();

    public DataTransformerRegistry() {
        registerAllTransformers();
    }

    public ErrorOr<EncodedByteBuffer> encodeData(ItemTransformingVersion version, List<ItemData> data) {
        List<UnsignedByte> bytes = new ArrayList<>();

        for (ItemData itemData : data) {
            try {
                ErrorOr<UnsignedByte[]> errorOrEncodedData = encodeData(version, itemData);
                if (errorOrEncodedData.hasError()) {
                    return ErrorOr.error(errorOrEncodedData.getError());
                }

                bytes.addAll(Arrays.asList(errorOrEncodedData.getValue()));
            } catch (Exception e) {
                return ErrorOr.<EncodedByteBuffer>error("Failed to encode data class "
                                + itemData.getClass().getSimpleName() + "!")
                        .logged();
            }
        }

        return ErrorOr.of(EncodedByteBuffer.fromBytes(bytes.toArray(new UnsignedByte[0])));
    }

    public ErrorOr<List<ItemData>> decodeData(EncodedByteBuffer encodedByteBuffer) {
        ArrayReader<UnsignedByte> byteReader = encodedByteBuffer.getReader();

        // Handle start data specially
        ErrorOr<StartData> errorOrStartData = StartDataTransformer.decodeData(byteReader);
        if (errorOrStartData.hasError()) {
            return ErrorOr.error(errorOrStartData.getError());
        }

        return decodeData(errorOrStartData.getValue().version(), byteReader);
    }

    private ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, ItemData data) {
        DataTransformer<ItemData> dataTransformer = (DataTransformer<ItemData>) dataTransformers.get(data.getClass());
        if (dataTransformer == null) {
            return ErrorOr.<UnsignedByte[]>error(
                            "No data transformer found for " + data.getClass().getSimpleName())
                    .logged();
        }

        return dataTransformer.encode(version, data);
    }

    private ErrorOr<List<ItemData>> decodeData(ItemTransformingVersion version, ArrayReader<UnsignedByte> byteReader) {
        List<ItemData> dataList = new ArrayList<>();

        while (byteReader.hasRemaining()) {
            UnsignedByte dataBlockId = byteReader.read();

            try {
                DataTransformer<ItemData> dataTransformer = dataTransformers.get(dataBlockId.toByte());

                if (dataTransformer == null) {
                    return ErrorOr.<List<ItemData>>error("No data transformer found for id " + dataBlockId.value())
                            .logged();
                }

                ErrorOr<ItemData> errorOrData = dataTransformer.decodeData(version, byteReader);

                if (errorOrData.hasError()) {
                    return ErrorOr.error(errorOrData.getError());
                }

                dataList.add(errorOrData.getValue());
            } catch (Exception e) {
                return ErrorOr.<List<ItemData>>error("Failed to decode data block with id " + dataBlockId.value() + "!")
                        .logged();
            }
        }

        boolean foundEndData = dataList.stream().anyMatch(data -> data instanceof EndData);
        if (!foundEndData) {
            return ErrorOr.error("No end data found in item data!");
        }
        dataList.removeIf(data -> data instanceof EndData);

        return ErrorOr.of(dataList);
    }

    private <T extends ItemData> void registerDataTransformer(Class<T> dataClass, DataTransformer<T> dataTransformer) {
        dataTransformers.put(dataClass, dataTransformer.getId(), dataTransformer);
    }

    private void registerAllTransformers() {
        registerDataTransformer(StartData.class, new StartDataTransformer());
        registerDataTransformer(TypeData.class, new TypeDataTransformer());
        registerDataTransformer(NameData.class, new NameDataTransformer());
        registerDataTransformer(EndData.class, new EndDataTransformer());
    }

    private static final class DataTransformerMap {
        private final Map<Class<? extends ItemData>, DataTransformer<? extends ItemData>> dataTransformers =
                new HashMap<>();

        private final Map<Byte, DataTransformer<? extends ItemData>> idToTransformerMap = new HashMap<>();

        public void put(
                Class<? extends ItemData> dataClass, byte id, DataTransformer<? extends ItemData> dataTransformer) {
            if (dataTransformers.put(dataClass, dataTransformer) != null) {
                throw new IllegalStateException("Duplicate data class: " + dataClass.getSimpleName());
            }
            if (idToTransformerMap.put(id, dataTransformer) != null) {
                throw new IllegalStateException("Duplicate id: " + id);
            }
        }

        public <T extends ItemData> DataTransformer<T> get(Class<T> dataClass) {
            return (DataTransformer<T>) dataTransformers.get(dataClass);
        }

        public <T extends ItemData> DataTransformer<T> get(byte id) {
            return (DataTransformer<T>) idToTransformerMap.get(id);
        }
    }
}

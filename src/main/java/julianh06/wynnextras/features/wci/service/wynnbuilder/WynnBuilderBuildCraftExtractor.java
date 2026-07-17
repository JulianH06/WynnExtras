package julianh06.wynnextras.features.wci.service.wynnbuilder;

import julianh06.wynnextras.features.wci.model.IngredientRequirement;

import java.util.ArrayList;
import java.util.List;

public final class WynnBuilderBuildCraftExtractor {
    private static final int VECTOR_FLAG_MIN = 12;
    private static final int VERSION_BITLEN = 10;
    private static final int EQUIPMENT_NUM = 9;
    private static final int EQUIPMENT_KIND_BITLEN = 2;
    private static final int EQUIPMENT_KIND_NORMAL = 0;
    private static final int EQUIPMENT_KIND_CRAFTED = 1;
    private static final int EQUIPMENT_KIND_CUSTOM = 2;
    private static final int ITEM_ID_BITLEN = 13;
    private static final int CUSTOM_STR_LENGTH_BITLEN = 12;
    private static final int EQUIPMENT_POWDERS_FLAG_BITLEN = 1;
    private static final int EQUIPMENT_POWDERS_HAS_POWDERS = 1;
    private static final int POWDER_ID_BITLEN = 6;
    private static final int POWDER_REPEAT_OP_BITLEN = 1;
    private static final int POWDER_REPEAT_OP_REPEAT = 0;
    private static final int POWDER_REPEAT_TIER_OP_BITLEN = 1;
    private static final int POWDER_REPEAT_TIER_OP_REPEAT_TIER = 0;
    private static final int POWDER_WRAPPER_BITLEN = 2;
    private static final int POWDER_CHANGE_OP_BITLEN = 1;
    private static final int POWDER_CHANGE_OP_NEW_POWDER = 0;
    private static final int MIN_BINARY_BUILD_HASH_CHARS = 26;

    private final WynnBuilderCraftParser craftParser;

    public WynnBuilderBuildCraftExtractor() {
        this.craftParser = new WynnBuilderCraftParser();
    }

    public boolean isBinaryBuildPayload(String payload) {
        String hash = normalize(payload);
        return !hash.isBlank()
                && !hash.startsWith("CR-")
                && hash.length() >= MIN_BINARY_BUILD_HASH_CHARS
                && hash.chars().allMatch(c -> WynnBuilderCraftParser.B64.indexOf(c) >= 0)
                && WynnBuilderCraftParser.B64.indexOf(hash.charAt(0)) >= VECTOR_FLAG_MIN;
    }

    public ExtractedCrafts extract(String payload) {
        String hash = normalize(payload);
        if (!isBinaryBuildPayload(hash)) {
            return ExtractedCrafts.empty();
        }

        try {
            WynnBuilderBitCursor cursor = new WynnBuilderBitCursor(hash);
            int binaryFlag = cursor.read(6);
            if (binaryFlag < VECTOR_FLAG_MIN) {
                return ExtractedCrafts.empty();
            }
            cursor.read(VERSION_BITLEN);

            List<IngredientRequirement> requirements = new ArrayList<>();
            int craftedItems = 0;
            for (int slot = 0; slot < EQUIPMENT_NUM; slot++) {
                int kind = cursor.read(EQUIPMENT_KIND_BITLEN);
                switch (kind) {
                    case EQUIPMENT_KIND_NORMAL -> cursor.skip(ITEM_ID_BITLEN);
                    case EQUIPMENT_KIND_CRAFTED -> {
                        WynnBuilderCraftParser.DecodedCraft craft = craftParser.decodeEmbedded(cursor);
                        craftedItems++;
                        craftParser.requirements(craft).ifPresent(requirements::addAll);
                    }
                    case EQUIPMENT_KIND_CUSTOM -> {
                        int customLengthBits = cursor.read(CUSTOM_STR_LENGTH_BITLEN);
                        cursor.skip(customLengthBits);
                    }
                    default -> throw new IllegalArgumentException("Unknown WynnBuilder equipment kind: " + kind);
                }

                if (isPowderableSlot(slot)
                        && cursor.read(EQUIPMENT_POWDERS_FLAG_BITLEN) == EQUIPMENT_POWDERS_HAS_POWDERS) {
                    skipPowders(cursor);
                }
            }
            return new ExtractedCrafts(requirements, craftedItems);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unable to decode WynnBuilder builder hash: " + e.getMessage(), e);
        }
    }

    private void skipPowders(WynnBuilderBitCursor cursor) {
        cursor.skip(POWDER_ID_BITLEN);
        int guard = 0;
        while (true) {
            if (++guard > 512) {
                throw new IllegalArgumentException("Powder data did not terminate");
            }
            int repeat = cursor.read(POWDER_REPEAT_OP_BITLEN);
            if (repeat == POWDER_REPEAT_OP_REPEAT) {
                continue;
            }

            int tierOp = cursor.read(POWDER_REPEAT_TIER_OP_BITLEN);
            if (tierOp == POWDER_REPEAT_TIER_OP_REPEAT_TIER) {
                cursor.skip(POWDER_WRAPPER_BITLEN);
                continue;
            }

            int change = cursor.read(POWDER_CHANGE_OP_BITLEN);
            if (change == POWDER_CHANGE_OP_NEW_POWDER) {
                cursor.skip(POWDER_ID_BITLEN);
                continue;
            }
            return;
        }
    }

    private static boolean isPowderableSlot(int slot) {
        return slot == 0 || slot == 1 || slot == 2 || slot == 3 || slot == 8;
    }

    private static String normalize(String payload) {
        String hash = payload == null ? "" : payload.trim();
        if (hash.contains("#")) {
            hash = hash.substring(hash.lastIndexOf('#') + 1);
        }
        return hash;
    }

    public record ExtractedCrafts(List<IngredientRequirement> requirements, int craftedItems) {
        public ExtractedCrafts {
            requirements = List.copyOf(requirements == null ? List.of() : requirements);
            craftedItems = Math.max(0, craftedItems);
        }

        public static ExtractedCrafts empty() {
            return new ExtractedCrafts(List.of(), 0);
        }
    }
}

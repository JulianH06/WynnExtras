package julianh06.wynnextras.features.crafting.wynnbuilder;

import julianh06.wynnextras.features.crafting.data.WynnDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class WynnBuilderBuildDecoder {
    private static final int VECTOR_FLAG_MIN = 12;
    private static final int VERSION_BIT_LENGTH = 10;
    private static final int EQUIPMENT_SLOTS = 9;
    private static final int NORMAL_ITEM = 0;
    private static final int CRAFTED_ITEM = 1;
    private static final int CUSTOM_ITEM = 2;
    private static final int MIN_BUILD_HASH_LENGTH = 26;

    private WynnBuilderBuildDecoder() {}

    public static boolean isBuildHash(String payload) {
        String hash = normalize(payload);
        return hash.length() >= MIN_BUILD_HASH_LENGTH
                && !hash.startsWith("CR-")
                && WynnBuilderBase64.isValid(hash)
                && WynnBuilderBase64.charToInt(hash.charAt(0)) >= VECTOR_FLAG_MIN;
    }

    public static List<DecodedCraft> decode(String payload, WynnDataService dataService) {
        String hash = normalize(payload);
        if (!isBuildHash(hash)) return List.of();
        if (dataService == null || dataService.getState() != WynnDataService.State.READY) {
            throw new IllegalStateException("Crafting data is not ready");
        }
        return decode(hash, recipeId -> {
            WynnDataService.RecipeData recipe = dataService.getRecipeByWynnBuilderId(recipeId);
            return recipe == null ? null : recipe.type().isWeapon();
        });
    }

    static List<DecodedCraft> decode(String hash, IntFunction<Boolean> weaponRecipeLookup) {
        try {
            WynnBuilderBitCursor cursor = new WynnBuilderBitCursor(hash);
            if (cursor.read(6) < VECTOR_FLAG_MIN) return List.of();
            cursor.read(VERSION_BIT_LENGTH);

            List<DecodedCraft> crafts = new ArrayList<>();
            for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) {
                int kind = cursor.read(2);
                switch (kind) {
                    case NORMAL_ITEM -> cursor.skip(13);
                    case CRAFTED_ITEM -> {
                        int craftStart = cursor.bitIndex();
                        DecodedCraft craft = WynnBuilderDecoder.decodeEmbedded(cursor);
                        if (craft == null) {
                            throw new IllegalArgumentException("Unsupported embedded WynnBuilder craft");
                        }
                        Boolean weaponRecipe = weaponRecipeLookup.apply(craft.recipeId());
                        if (weaponRecipe == null) {
                            throw new IllegalArgumentException("Unknown WynnBuilder recipe ID: " + craft.recipeId());
                        }
                        WynnBuilderDecoder.finishEmbedded(cursor, craftStart, weaponRecipe);
                        crafts.add(craft);
                    }
                    case CUSTOM_ITEM -> cursor.skip(cursor.read(12));
                    default -> throw new IllegalArgumentException("Unknown WynnBuilder equipment kind: " + kind);
                }

                if (isPowderableSlot(slot) && cursor.read(1) == 1) {
                    skipPowders(cursor);
                }
            }
            return List.copyOf(crafts);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unable to decode WynnBuilder build: " + ex.getMessage(), ex);
        }
    }

    private static void skipPowders(WynnBuilderBitCursor cursor) {
        cursor.skip(6);
        for (int guard = 0; guard < 512; guard++) {
            if (cursor.read(1) == 0) continue;
            if (cursor.read(1) == 0) {
                cursor.skip(2);
                continue;
            }
            if (cursor.read(1) == 0) {
                cursor.skip(6);
                continue;
            }
            return;
        }
        throw new IllegalArgumentException("Powder data did not terminate");
    }

    private static boolean isPowderableSlot(int slot) {
        return slot == 0 || slot == 1 || slot == 2 || slot == 3 || slot == 8;
    }

    private static String normalize(String payload) {
        String hash = payload == null ? "" : payload.trim();
        if (hash.contains("#")) hash = hash.substring(hash.lastIndexOf('#') + 1);
        return hash;
    }
}

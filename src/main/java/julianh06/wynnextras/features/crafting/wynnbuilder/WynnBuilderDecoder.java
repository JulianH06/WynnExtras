package julianh06.wynnextras.features.crafting.wynnbuilder;

public class WynnBuilderDecoder {
    public static final int SUPPORTED_ENCODING_VERSION = 2;
    private static final int NO_INGREDIENT_ID = 4000;

    public static DecodedCraft decode(String input) {
        if (input == null || input.isBlank()) return null;

        input = input.trim();

        // URL format: extract hash after #
        if (input.contains("#")) {
            int hashIdx = input.lastIndexOf('#');
            input = input.substring(hashIdx + 1);
        }

        // Strip CR- prefix if present (used as marker in URLs and standalone)
        if (input.startsWith("CR-")) {
            input = input.substring(3);
        }

        if (input.isEmpty() || !WynnBuilderBase64.isValid(input)) return null;

        if (input.length() < 16) return null;

        try {
            return input.charAt(0) == '1' ? decodeLegacy(input) : decodeBitPacked(new WynnBuilderBitCursor(input), false);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public static DecodedCraft decodeEmbedded(WynnBuilderBitCursor cursor) {
        return decodeBitPacked(cursor, true);
    }

    public static void finishEmbedded(WynnBuilderBitCursor cursor, int startBit, boolean weaponRecipe) {
        if (weaponRecipe) cursor.skip(4);
        int remainder = (cursor.bitIndex() - startBit) % 6;
        cursor.skip(remainder == 0 ? 6 : 6 - remainder);
    }

    private static DecodedCraft decodeLegacy(String hash) {
        int[] ingredientIds = new int[6];
        for (int i = 0; i < 6; i++) {
            ingredientIds[i] = readLegacyPair(hash, 1 + i * 2);
        }
        int recipeId = readLegacyPair(hash, 13);
        int tierCombo = WynnBuilderBase64.charToInt(hash.charAt(15));
        if (tierCombo < 1 || tierCombo > 9) return null;
        int mat1Tier = tierCombo % 3 == 0 ? 3 : tierCombo % 3;
        int mat2Tier = (int) Math.floor((tierCombo - 0.5) / 3.0) + 1;
        return new DecodedCraft(ingredientIds, recipeId, mat1Tier, mat2Tier, 0);
    }

    private static int readLegacyPair(String hash, int index) {
        return WynnBuilderBase64.charToInt(hash.charAt(index)) * 64
                + WynnBuilderBase64.charToInt(hash.charAt(index + 1));
    }

    private static DecodedCraft decodeBitPacked(WynnBuilderBitCursor cursor, boolean embedded) {
        if (cursor.read(1) != 0) return null;
        int version = cursor.read(7);
        if (version != SUPPORTED_ENCODING_VERSION) return null;
        int[] ingredientIds = new int[6];
        for (int i = 0; i < ingredientIds.length; i++) ingredientIds[i] = cursor.read(12);
        int recipeId = cursor.read(12);
        int mat1Tier = cursor.read(3) + 1;
        int mat2Tier = cursor.read(3) + 1;
        int attackSpeed = embedded ? 0 : cursor.read(4);
        return new DecodedCraft(ingredientIds, recipeId, mat1Tier, mat2Tier, attackSpeed);
    }

    public static boolean isNoIngredient(int id) {
        return id == 0 || id == NO_INGREDIENT_ID;
    }
}

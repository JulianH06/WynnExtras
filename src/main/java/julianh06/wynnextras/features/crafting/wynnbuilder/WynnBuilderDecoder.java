package julianh06.wynnextras.features.crafting.wynnbuilder;

public class WynnBuilderDecoder {

    // WynnBuilder exposes these only as protocol constants in js/craft.js and js/load_ing.js.
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

        if (input.length() >= 16) {
            return decodeNewFormat(input);
        }

        return null;
    }

    private static DecodedCraft decodeNewFormat(String hash) {
        // Convert hash to a bit array
        // Each base64 char = 6 bits, stored LSB first within each char
        int totalBits = hash.length() * 6;
        int[] bits = new int[totalBits];

        for (int i = 0; i < hash.length(); i++) {
            int val = WynnBuilderBase64.charToInt(hash.charAt(i));
            for (int b = 0; b < 6; b++) {
                bits[i * 6 + b] = (val >> b) & 1;
            }
        }

        int cursor = 0;

        // Legacy flag (1 bit)
        int legacy = readBits(bits, cursor, 1);
        cursor += 1;

        if (legacy == 1) return null;

        // Version (7 bits)
        int version = readBits(bits, cursor, 7);
        cursor += 7;
        if (version != SUPPORTED_ENCODING_VERSION) return null;

        // 6 ingredient IDs (12 bits each)
        int[] ingredientIds = new int[6];
        for (int i = 0; i < 6; i++) {
            ingredientIds[i] = readBits(bits, cursor, 12);
            cursor += 12;
        }

        // Recipe ID (12 bits)
        int recipeId = readBits(bits, cursor, 12);
        cursor += 12;

        // 2 material tiers (3 bits each, stored as tier-1)
        int mat1Tier = readBits(bits, cursor, 3) + 1;
        cursor += 3;
        int mat2Tier = readBits(bits, cursor, 3) + 1;
        cursor += 3;

        int attackSpeed = readBits(bits, cursor, 4);

        return new DecodedCraft(ingredientIds, recipeId, mat1Tier, mat2Tier, attackSpeed);
    }

    /**
     * Read bits from LSB-first bit array.
     */
    private static int readBits(int[] bits, int start, int count) {
        int result = 0;
        for (int i = 0; i < count; i++) {
            if (start + i < bits.length) {
                result |= bits[start + i] << i;
            }
        }
        return result;
    }

    public static boolean isNoIngredient(int id) {
        return id == NO_INGREDIENT_ID;
    }
}

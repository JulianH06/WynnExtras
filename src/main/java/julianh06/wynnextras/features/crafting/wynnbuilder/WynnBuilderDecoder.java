package julianh06.wynnextras.features.crafting.wynnbuilder;

public class WynnBuilderDecoder {

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

        // Try legacy format first (version char '1', length 16+)
        if (input.charAt(0) == '1' && input.length() >= 16) {
            DecodedCraft legacy = decodeLegacyFormat(input);
            if (legacy != null) return legacy;
        }

        // Try new bit-packed format
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

        if (legacy == 1) {
            // Legacy bit set - this is a legacy hash without CR- prefix
            return decodeLegacyFormat(hash);
        }

        // Version (7 bits)
        int version = readBits(bits, cursor, 7);
        cursor += 7;

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

        // Attack speed (4 bits) - we read it but don't use it for auto-fill
        // Only present for weapons, but we skip it regardless

        return new DecodedCraft(ingredientIds, recipeId, mat1Tier, mat2Tier);
    }

    private static DecodedCraft decodeLegacyFormat(String data) {
        if (data.length() < 16) return null;

        // Version char
        char version = data.charAt(0);
        if (version != '1') return null;

        data = data.substring(1);

        // 6 ingredient IDs (2 chars each = 12 chars)
        int[] ingredientIds = new int[6];
        for (int i = 0; i < 6; i++) {
            ingredientIds[i] = WynnBuilderBase64.toInt(data.substring(2 * i, 2 * i + 2));
        }

        // Recipe ID (2 chars)
        int recipeId = WynnBuilderBase64.toInt(data.substring(12, 14));

        // Tier combo (1 char)
        int tierNum = WynnBuilderBase64.toInt(data.substring(14, 15));
        int mat1Tier = tierNum % 3 == 0 ? 3 : tierNum % 3;
        int mat2Tier = (int) Math.floor((tierNum - 0.5) / 3) + 1;

        return new DecodedCraft(ingredientIds, recipeId, mat1Tier, mat2Tier);
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
        return id == NO_INGREDIENT_ID || id == 0;
    }
}

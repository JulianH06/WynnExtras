package julianh06.wynnextras.wynncraft.codec;

/** Decoder for standalone and build-embedded WynnCrafter payloads. */
public final class WynnBuilderCraftCodec {
    public static final int SUPPORTED_ENCODING_VERSION = 2;
    private static final int NO_INGREDIENT_ID = 4000;

    private WynnBuilderCraftCodec() {}

    public static WynnBuilderCraft decode(String input) {
        String hash = WynnBuilderEncoding.craftHash(input);
        if (hash.isEmpty() || hash.length() < 16 || !WynnBuilderEncoding.isValid(hash)) return null;
        try {
            return hash.charAt(0) == '1' ? decodeLegacy(hash)
                    : decodeEmbedded(new WynnBuilderBitCursor(hash), false);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String encode(WynnBuilderCraft craft, boolean weapon) {
        WynnBuilderBitVector bits = new WynnBuilderBitVector().append(4, 8);
        for (int ingredient : craft.ingredientIds()) bits.append(ingredient, 12);
        bits.append(craft.recipeId(), 12)
                .append(craft.material1Tier() - 1, 3)
                .append(craft.material2Tier() - 1, 3);
        if (weapon) bits.append(craft.attackSpeed(), 4);
        return bits.padToNextCharacter().toBase64();
    }

    static WynnBuilderCraft decodeEmbedded(WynnBuilderBitCursor cursor, boolean embedded) {
        if (cursor.read(1) != 0 || cursor.read(7) != SUPPORTED_ENCODING_VERSION) return null;
        int[] ingredients = new int[6];
        for (int i = 0; i < ingredients.length; i++) ingredients[i] = cursor.read(12);
        int recipe = cursor.read(12);
        int material1 = cursor.read(3) + 1;
        int material2 = cursor.read(3) + 1;
        int speed = embedded ? 0 : cursor.read(4);
        return new WynnBuilderCraft(ingredients, recipe, material1, material2, speed);
    }

    static WynnBuilderCraft finishEmbedded(
            WynnBuilderBitCursor cursor, int startBit, WynnBuilderCraft craft, boolean weaponRecipe
    ) {
        int speed = weaponRecipe ? cursor.read(4) : 0;
        int remainder = (cursor.bitIndex() - startBit) % 6;
        cursor.skip(remainder == 0 ? 6 : 6 - remainder);
        return new WynnBuilderCraft(craft.ingredientIds(), craft.recipeId(), craft.material1Tier(),
                craft.material2Tier(), speed);
    }

    public static boolean isNoIngredient(int id) {
        return id == 0 || id == NO_INGREDIENT_ID;
    }

    private static WynnBuilderCraft decodeLegacy(String hash) {
        int[] ingredients = new int[6];
        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i] = WynnBuilderEncoding.bigEndianValue(hash, 1 + i * 2, 2);
        }
        int recipe = WynnBuilderEncoding.bigEndianValue(hash, 13, 2);
        int tiers = WynnBuilderEncoding.value(hash.charAt(15));
        if (tiers < 1 || tiers > 9) return null;
        int material1 = tiers % 3 == 0 ? 3 : tiers % 3;
        int material2 = (int) Math.floor((tiers - 0.5) / 3.0) + 1;
        int speed = hash.length() > 16 ? WynnBuilderEncoding.value(hash.charAt(16)) : 0;
        return new WynnBuilderCraft(ingredients, recipe, material1, material2, speed);
    }
}

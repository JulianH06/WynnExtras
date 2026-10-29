package julianh06.wynnextras.wynncraft.codec;

/** Shared alphabet and URL handling for every WynnBuilder codec in WynnExtras. */
public final class WynnBuilderEncoding {
    public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-";
    private static final int[] VALUES = new int[128];

    static {
        java.util.Arrays.fill(VALUES, -1);
        for (int i = 0; i < ALPHABET.length(); i++) VALUES[ALPHABET.charAt(i)] = i;
    }

    private WynnBuilderEncoding() {}

    public static int value(char character) {
        if (character >= VALUES.length || VALUES[character] < 0) {
            throw new IllegalArgumentException("Invalid WynnBuilder Base64 character: " + character);
        }
        return VALUES[character];
    }

    public static boolean isValid(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character >= VALUES.length || VALUES[character] < 0) return false;
        }
        return true;
    }

    public static int bigEndianValue(String value, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) result = result * 64 + value(value.charAt(offset + i));
        return result;
    }

    public static String hash(String input) {
        String hash = input == null ? "" : input.trim();
        int marker = hash.lastIndexOf('#');
        if (marker >= 0) hash = hash.substring(marker + 1);
        return hash;
    }

    public static String craftHash(String input) {
        String hash = hash(input);
        return hash.startsWith("CR-") ? hash.substring(3) : hash;
    }
}

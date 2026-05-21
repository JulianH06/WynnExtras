package julianh06.wynnextras.features.crafting.wynnbuilder;

public class WynnBuilderBase64 {
    private static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-";
    private static final int[] CHAR_TO_VALUE = new int[128];

    static {
        java.util.Arrays.fill(CHAR_TO_VALUE, -1);
        for (int i = 0; i < DIGITS.length(); i++) {
            CHAR_TO_VALUE[DIGITS.charAt(i)] = i;
        }
    }

    public static int toInt(String chars) {
        int result = 0;
        for (int i = 0; i < chars.length(); i++) {
            result = (result << 6) + charToInt(chars.charAt(i));
        }
        return result;
    }

    public static int charToInt(char c) {
        if (c >= 128 || CHAR_TO_VALUE[c] == -1) {
            throw new IllegalArgumentException("Invalid WynnBuilder Base64 character: " + c);
        }
        return CHAR_TO_VALUE[c];
    }

    public static boolean isValid(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 128 || CHAR_TO_VALUE[c] == -1) return false;
        }
        return true;
    }
}

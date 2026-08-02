package julianh06.wynnextras.features.shoppinglist.service;

import java.util.regex.Pattern;

public final class ShoppingListTextCleaner {
    private static final Pattern MINECRAFT_FORMAT_CODE = Pattern.compile("\u00A7.");
    private static final Pattern MOJIBAKE_FORMAT_CODE = Pattern.compile("\u00C2\u00A7.");
    private static final Pattern HEX_COLOR_TAG = Pattern.compile("&#[0-9a-fA-F]{6,8}");
    private static final Pattern NAMED_COLOR_TAG = Pattern.compile("&\\{[^}]+}");
    private static final Pattern CONTROL_OR_PRIVATE_GLYPH = Pattern.compile("[\\p{Cc}\\p{Cf}\\p{Co}]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ShoppingListTextCleaner() {}

    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = MOJIBAKE_FORMAT_CODE.matcher(text).replaceAll("");
        cleaned = MINECRAFT_FORMAT_CODE.matcher(cleaned).replaceAll("");
        cleaned = HEX_COLOR_TAG.matcher(cleaned).replaceAll("");
        cleaned = NAMED_COLOR_TAG.matcher(cleaned).replaceAll("");
        cleaned = CONTROL_OR_PRIVATE_GLYPH.matcher(cleaned).replaceAll("");
        return WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
    }
}

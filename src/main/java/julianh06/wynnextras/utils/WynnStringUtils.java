package julianh06.wynnextras.utils;

public final class WynnStringUtils {
    private WynnStringUtils() {}

    public static String normalizeBadString(String value) {
        if (value == null) return "";
        return value.replace("ÀÀÀ", "").replace("À", "").replace("֎", "").replace('’', '\'');
    }
}

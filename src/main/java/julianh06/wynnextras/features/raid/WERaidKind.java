package julianh06.wynnextras.features.raid;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public enum WERaidKind {
    NOTG("NOG", "Nest of the Grootslangs"),
    NOL("NOL", "Orphion's Nexus of Light"),
    TCC("TCC", "The Canyon Colossus"),
    TNA("TNA", "The Nameless Anomaly"),
    TWP("WTP", "The Wartorn Palace"),
    UNKNOWN("UNKNOWN", "Unknown Raid");

    private final String abbreviation;
    private final String displayName;

    WERaidKind(String abbreviation, String displayName) {
        this.abbreviation = abbreviation;
        this.displayName = displayName;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String displayName() {
        return displayName;
    }

    public static WERaidKind from(String abbreviation, String displayName) {
        String normalized = abbreviation == null ? "" : abbreviation.toUpperCase(java.util.Locale.ROOT);
        if (normalized.equals("TWP") || normalized.equals("WTP")) return TWP;
        for (WERaidKind kind : values()) {
            if (kind.abbreviation.equals(normalized) || kind.displayName.equals(displayName)) return kind;
        }
        return UNKNOWN;
    }

    public static WERaidKind fromEntryTitle(Text title) {
        if (title == null) return UNKNOWN;
        List<TitlePart> parts = titleParts(title);
        if (parts.equals(List.of(part("Nest of The Grootslangs", Formatting.DARK_GREEN, false, false)))) return NOTG;
        if (parts.equals(List.of(
                part("Orphion's Nexus of ", Formatting.WHITE, false, true),
                part("Light", Formatting.WHITE, true, true)))) return NOL;
        if (parts.equals(List.of(part("The Canyon Colossus", 0x5f968b, false, false)))) return TCC;
        if (parts.equals(List.of(
                part("The ", Formatting.BLUE, true, false),
                part("Nameless", Formatting.DARK_BLUE, true, true),
                part(" Anomaly", Formatting.BLUE, true, false)))) return TNA;
        if (parts.equals(List.of(part("The Wartorn Palace", 0x00f010, false, false)))) return TWP;
        return UNKNOWN;
    }

    private static List<TitlePart> titleParts(Text title) {
        List<TitlePart> parts = new ArrayList<>();
        boolean[] valid = {true};
        title.visit((style, string) -> {
            if (string.isEmpty()) return Optional.empty();
            if (!hasOnlyEntryTitleStyle(style) || style.getColor() == null) {
                valid[0] = false;
                return Optional.empty();
            }
            parts.add(new TitlePart(string, style.getColor().getRgb(), style.isBold(), style.isObfuscated()));
            return Optional.empty();
        }, Style.EMPTY);
        return valid[0] ? List.copyOf(parts) : List.of();
    }

    private static boolean hasOnlyEntryTitleStyle(Style style) {
        return !style.isItalic()
                && !style.isUnderlined()
                && !style.isStrikethrough()
                && style.getClickEvent() == null
                && style.getHoverEvent() == null
                && Objects.equals(style.getFont(), Style.EMPTY.getFont());
    }

    private static TitlePart part(String text, Formatting color, boolean bold, boolean obfuscated) {
        return part(text, color.getColorValue(), bold, obfuscated);
    }

    private static TitlePart part(String text, int color, boolean bold, boolean obfuscated) {
        return new TitlePart(text, color, bold, obfuscated);
    }

    private record TitlePart(String text, int color, boolean bold, boolean obfuscated) {}
}

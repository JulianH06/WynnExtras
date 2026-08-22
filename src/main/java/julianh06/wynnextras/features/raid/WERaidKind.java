package julianh06.wynnextras.features.raid;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

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
        Deque<TextNode> remaining = new ArrayDeque<>();
        remaining.push(new TextNode(title, Style.EMPTY));

        while (!remaining.isEmpty()) {
            TextNode node = remaining.pop();
            Text component = node.component();
            String content = MutableText.of(component.getContent()).getString();
            if (!addCodedParts(parts, content, component.getStyle(), node.parentStyle())) return List.of();

            Style childStyle = component.getStyle().withParent(node.parentStyle());
            List<Text> siblings = component.getSiblings();
            for (int i = siblings.size() - 1; i >= 0; i--) {
                remaining.push(new TextNode(siblings.get(i), childStyle));
            }
        }
        return List.copyOf(parts);
    }

    private static boolean addCodedParts(List<TitlePart> parts, String content, Style componentStyle,
                                         Style parentStyle) {
        Style style = componentStyle;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char character = content.charAt(i);
            if (character != Formatting.FORMATTING_CODE_PREFIX || i + 1 >= content.length()) {
                text.append(character);
                continue;
            }

            if (content.charAt(i + 1) == '#' && i + 9 < content.length()) {
                String rgba = content.substring(i + 2, i + 10);
                if (rgba.chars().allMatch(WERaidKind::isHexDigit)) {
                    if (!addPart(parts, text, style.withParent(parentStyle))) return false;
                    style = Style.EMPTY.withColor(Integer.parseInt(rgba.substring(0, 6), 16));
                    i += 9;
                    continue;
                }
            }

            Formatting formatting = Formatting.byCode(content.charAt(i + 1));
            if (formatting == null) {
                text.append(character);
                continue;
            }
            if (!addPart(parts, text, style.withParent(parentStyle))) return false;
            style = formatting == Formatting.RESET
                    ? Style.EMPTY
                    : formatting.isColor()
                            ? Style.EMPTY.withColor(formatting)
                            : style.withFormatting(formatting);
            i++;
        }
        return addPart(parts, text, style.withParent(parentStyle));
    }

    private static boolean addPart(List<TitlePart> parts, StringBuilder text, Style style) {
        if (text.isEmpty()) return true;
        if (!hasOnlyEntryTitleStyle(style) || style.getColor() == null) return false;
        parts.add(new TitlePart(text.toString(), style.getColor().getRgb(), style.isBold(), style.isObfuscated()));
        text.setLength(0);
        return true;
    }

    private static boolean isHexDigit(int character) {
        return Character.digit(character, 16) >= 0;
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

    private record TextNode(Text component, Style parentStyle) {}
    private record TitlePart(String text, int color, boolean bold, boolean obfuscated) {}
}

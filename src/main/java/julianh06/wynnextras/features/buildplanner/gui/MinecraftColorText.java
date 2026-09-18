package julianh06.wynnextras.features.buildplanner.gui;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class MinecraftColorText {
    private MinecraftColorText() {
    }

    static Text parse(String input) {
        String value = input == null ? "" : input;
        MutableText result = Text.empty();
        StringBuilder segment = new StringBuilder();
        Formatting current = Formatting.WHITE;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character == '&' || character == '\u00a7') && index + 1 < value.length()) {
                Formatting next = Formatting.byCode(value.charAt(index + 1));
                if (next != null) {
                    append(result, segment, current);
                    current = next == Formatting.RESET ? Formatting.WHITE : next;
                    index++;
                    continue;
                }
            }
            segment.append(character);
        }
        append(result, segment, current);
        return result;
    }

    static String plain(String input) {
        String value = input == null ? "" : input;
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character == '&' || character == '\u00a7')
                    && index + 1 < value.length()
                    && Formatting.byCode(value.charAt(index + 1)) != null) {
                index++;
                continue;
            }
            result.append(character);
        }
        return result.toString();
    }

    private static void append(
            MutableText result, StringBuilder segment, Formatting formatting
    ) {
        if (segment.isEmpty()) {
            return;
        }
        result.append(Text.literal(segment.toString()).formatted(formatting));
        segment.setLength(0);
    }
}

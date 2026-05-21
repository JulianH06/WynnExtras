package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackDuplicateMessages {
    private static final Pattern COUNTER_SUFFIX = Pattern.compile("\\s*\\((\\d+)\\)\\s*$");

    // Authoritative count tracked in-memory to avoid re-parsing chat lines.
    private static String lastStackedText = null;
    private static int lastStackedCount = 1;
    private static int lastStackedTick = -1;

    public static Text process(Text message) {
        if (!WynnExtrasConfig.INSTANCE.stackDuplicateMessages) return message;
        try {
            ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
            ChatHudAccessor acc = (ChatHudAccessor) chatHud;
            List<ChatHudLine> messages = acc.getMessages();
            List<ChatHudLine.Visible> visible = acc.getVisibleMessages();
            String newMsg = strip(message.getString());
            if (newMsg.isEmpty() || messages.isEmpty()) return message;

            int windowTicks = Math.max(1, WynnExtrasConfig.INSTANCE.stackDuplicateWindowMinutes) * 60 * 20;
            int currentTick = MinecraftClient.getInstance().inGameHud.getTicks();

            // Scan the window for an exact-match duplicate of the incoming message.
            int matchIdx = -1;
            for (int i = 0; i < messages.size(); i++) {
                if (currentTick - messages.get(i).creationTick() > windowTicks) break;
                if (strip(messages.get(i).content().getString()).equals(newMsg)) {
                    matchIdx = i;
                    break;
                }
            }

            boolean trackerSaysStack = newMsg.equals(lastStackedText)
                    && lastStackedTick >= 0
                    && currentTick - lastStackedTick <= windowTicks;

            if (matchIdx == -1 && !trackerSaysStack) {
                // Truly first occurrence — reset tracker.
                lastStackedText = newMsg;
                lastStackedCount = 1;
                lastStackedTick = currentTick;
                return message;
            }

            // Determine the new count:
            //   - If our tracker says we recently stacked this, use tracker+1 (authoritative).
            //   - Otherwise fall back to parsing the count off the chat line we found.
            int newCount;
            if (trackerSaysStack) {
                newCount = lastStackedCount + 1;
            } else {
                newCount = extractCount(messages.get(matchIdx).content().getString()) + 1;
            }

            // Remove the existing stacked entry if we can find one in chat. If it was
            // evicted or the scan didn't find it, we just skip removal — the tracker
            // keeps the count going so a new stacked entry still gets the right number.
            if (matchIdx != -1) {
                removeVisibleLinesForMessage(visible, matchIdx);
                messages.remove(matchIdx);
            }

            lastStackedText = newMsg;
            lastStackedCount = newCount;
            lastStackedTick = currentTick;

            MutableText wrapped = Text.empty().append(message);
            wrapped.append(Text.literal((message.getString().endsWith(" ") ? "" : " ") + String.format("§7(%d)", newCount)));
            return wrapped;
        } catch (Exception ignored) {}
        return message;
    }

    // Removes all wrapped visible lines belonging to messages[matchIdx].
    // In vanilla ChatHud, each ChatHudLine contributes a contiguous block of Visible
    // entries: starts with an endOfEntry=true line (the topmost wrapped line) followed
    // by zero or more endOfEntry=false continuation lines. To locate messages[matchIdx]
    // we count endOfEntry=true markers; from that marker up to (but not including) the
    // next one belongs to this message.
    private static void removeVisibleLinesForMessage(List<ChatHudLine.Visible> visible, int matchIdx) {
        int msgIdx = -1;
        int start = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).endOfEntry()) {
                msgIdx++;
                if (msgIdx == matchIdx) {
                    start = i;
                    break;
                }
            }
        }
        if (start == -1) return;

        // Remove the endOfEntry=true marker itself, then any continuation wraps
        // belonging to this message (stop as soon as we hit the next endOfEntry=true).
        visible.remove(start);
        while (start < visible.size() && !visible.get(start).endOfEntry()) {
            visible.remove(start);
        }
    }

    private static int extractCount(String raw) {
        String stripped = raw.replaceAll("§[0-9a-fk-orx]", "");
        Matcher m = COUNTER_SUFFIX.matcher(stripped);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private static String strip(String s) {
        if (s == null) return "";
        // Include 'x' so BungeeCord hex prefix sequences are also stripped.
        String out = s.replaceAll("§[0-9a-fk-orx]", "");
        out = COUNTER_SUFFIX.matcher(out).replaceAll("");
        // Strip Wynncraft's PUA icon glyphs (rank pills, badges, etc.) so messages with
        // and without a player rank prefix are recognized as the same text.
        out = out.replaceAll("[\uE000-\uF8FF]", "");
        // Drop everything in supplementary planes (where Wynncraft's RP glyphs live).
        StringBuilder sb = new StringBuilder(out.length());
        int i = 0;
        while (i < out.length()) {
            int cp = out.codePointAt(i);
            int step = Character.charCount(cp);
            if (cp < 0x10000) sb.appendCodePoint(cp);
            i += step;
        }
        out = sb.toString();
        // Normalize all Unicode whitespace (incl. non-breaking spaces) so spacing
        // differences don't block a duplicate match.
        out = out.replaceAll("(?U)\\s+", " ").trim();
        return out;
    }
}

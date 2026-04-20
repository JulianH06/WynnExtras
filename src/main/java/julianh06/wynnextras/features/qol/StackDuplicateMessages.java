package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
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

            if (matchIdx == -1) {
                // Fresh message — reset tracker.
                lastStackedText = newMsg;
                lastStackedCount = 1;
                lastStackedTick = currentTick;
                return message;
            }

            // Determine the new count from our own tracked state so formatting
            // quirks in the chat line can't corrupt it.
            int newCount;
            if (newMsg.equals(lastStackedText) && lastStackedTick >= 0
                    && currentTick - lastStackedTick <= windowTicks) {
                newCount = lastStackedCount + 1;
            } else {
                // State was lost (config reload / first run after restart) —
                // fall back to whatever count the chat line claims.
                newCount = 2;
            }

            removeVisibleLinesForMessage(visible, matchIdx);
            messages.remove(matchIdx);

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
    private static void removeVisibleLinesForMessage(List<ChatHudLine.Visible> visible, int matchIdx) {
        int msgIdx = 0;
        int i = 0;
        while (i < visible.size() && msgIdx < matchIdx) {
            if (visible.get(i).endOfEntry()) msgIdx++;
            i++;
        }
        // Now i points at the first wrapped line of messages[matchIdx].
        while (i < visible.size()) {
            boolean last = visible.get(i).endOfEntry();
            visible.remove(i);
            if (last) break;
        }
    }

    private static String strip(String s) {
        if (s == null) return "";
        String out = s.replaceAll("§[0-9a-fk-or]", "");
        out = COUNTER_SUFFIX.matcher(out).replaceAll("");
        return out;
    }
}

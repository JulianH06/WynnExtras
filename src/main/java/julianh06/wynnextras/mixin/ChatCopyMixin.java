package julianh06.wynnextras.mixin;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatCopyMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onRightClickCopy(Click click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!WynnExtrasConfig.INSTANCE.rightClickToCopyChat) return;
        if (click.button() != 1) return; // right mouse button only

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ChatHud chatHud = mc.inGameHud.getChatHud();
        ChatHudAccessor acc = (ChatHudAccessor) chatHud;

        if (!isInChatArea(click.x(), click.y())) return;

        List<ChatHudLine.Visible> visible = acc.getVisibleMessages();
        if (visible.isEmpty()) return;

        // Approximate line index from cursor Y. Chat renders bottom-up just above the chat input.
        double chatScale = mc.options.getChatScale().getValue();
        double lineSpacing = mc.options.getChatLineSpacing().getValue();
        double lineHeightPx = 9.0 * (lineSpacing + 1.0) * chatScale;
        int screenHeight = mc.getWindow().getScaledHeight();
        // Chat bottom is ~40 px above the screen bottom when focused (the input line sits below).
        double chatBottomY = screenHeight - 40;
        double dy = chatBottomY - click.y();
        if (dy < 0) return;
        int lineFromBottom = (int) (dy / lineHeightPx);
        int lineIdx = lineFromBottom + acc.getScrolledLines();
        if (lineIdx < 0 || lineIdx >= visible.size()) return;

        // Walk backward to the top of the wrapped message (endOfEntry=true marker).
        int topIdx = lineIdx;
        while (topIdx > 0 && !visible.get(topIdx).endOfEntry()) topIdx--;

        // Count endOfEntry=true markers up to (and including) topIdx to find
        // the corresponding ChatHudLine index in the messages list.
        int msgIdx = -1;
        for (int i = 0; i <= topIdx; i++) {
            if (visible.get(i).endOfEntry()) msgIdx++;
        }
        List<ChatHudLine> messages = acc.getMessages();
        if (msgIdx < 0 || msgIdx >= messages.size()) return;

        String raw = messages.get(msgIdx).content().getString();
        String clean = stripPuaAndFormatting(raw).trim();
        if (clean.isEmpty()) return;

        mc.keyboard.setClipboard(clean);
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§aCopied to clipboard: §f" + clean)));

        cir.setReturnValue(true);
    }

    /** Strip Minecraft §-format codes and Wynncraft's custom PUA icon codepoints
     *  (pill prefix, badges, symbols) so the clipboard contains just readable text. */
    private static String stripPuaAndFormatting(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int step = Character.charCount(cp);
            // Skip §x formatting pair (legacy color codes).
            if (cp == '\u00a7' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if ("0123456789abcdefABCDEFklmnorxKLMNORX".indexOf(next) >= 0) {
                    i += 2;
                    continue;
                }
            }
            // Strip everything the resource pack abuses for custom glyphs: BMP Private-Use
            // Area plus EVERY supplementary-plane codepoint. Emoji would also be stripped
            // but they're essentially never in Wynncraft chat anyway, and this guarantees
            // no stray Wynncraft icon codepoints slip through no matter what plane they
            // sit in.
            if ((cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0x10000) {
                i += step;
                continue;
            }
            sb.appendCodePoint(cp);
            i += step;
        }
        return sb.toString();
    }

    private static boolean isInChatArea(double x, double y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sh = mc.getWindow().getScaledHeight();
        int sw = mc.getWindow().getScaledWidth();
        // Chat occupies the bottom-left portion of the screen.
        return x >= 0 && x <= sw * 0.6 && y >= sh * 0.4 && y <= sh - 30;
    }
}

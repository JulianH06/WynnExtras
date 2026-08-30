package julianh06.wynnextras.features.chat;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Plays a short sound when another player sends the local player a direct message.
 *
 * <p>Wynncraft colours a direct message with its own tint, which is what separates it from guild
 * or party chat. Both halves of a conversation carry that tint, so the sender is read from in
 * front of the first colon and compared against the local player to keep only the incoming half.
 */
@WEModule
public class PrivateMessageSound {

    /** The tint Wynncraft gives direct messages. */
    private static final int PRIVATE_MESSAGE_COLOR = 0xDDCC99;

    @SubscribeEvent
    void onChatMessage(ChatEvent event) {
        if (!WynnExtrasConfig.INSTANCE.privateMessageSound) return;
        if (event.message == null) return;
        if (!isPrivateMessage(event.message)) return;

        String sender = senderOf(event.message.getString());
        if (sender.isEmpty()) return;
        if (sender.equalsIgnoreCase(MinecraftUtils.playerName())) return;   // our own outgoing message

        MinecraftUtils.playSoundAmbient(
                SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.privateMessageSoundType.getSoundId())),
                WynnExtrasConfig.INSTANCE.privateMessageSoundVolume / 100,
                WynnExtrasConfig.INSTANCE.privateMessageSoundPitch / 100);
    }

    private static boolean isPrivateMessage(Text message) {
        TextColor color = firstColor(message);
        return color != null && (color.getRgb() & 0xFFFFFF) == PRIVATE_MESSAGE_COLOR;
    }

    /** Colour of the first styled part of the line, which is the one carrying the channel tint. */
    private static TextColor firstColor(Text text) {
        TextColor own = text.getStyle().getColor();
        if (own != null) return own;
        for (Text sibling : text.getSiblings()) {
            TextColor found = firstColor(sibling);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * The plain line reads "&lt;arrow&gt; recipient &lt;badges&gt; sender: message", so the sender is
     * the last token before the first colon. Rank icons and banner glyphs sit in the private use
     * area and are dropped.
     */
    private static String senderOf(String plain) {
        int colon = plain.indexOf(':');
        if (colon <= 0) return "";

        String beforeColon = plain.substring(0, colon);
        int lastSpace = beforeColon.lastIndexOf(' ');
        String token = lastSpace >= 0 ? beforeColon.substring(lastSpace + 1) : beforeColon;

        StringBuilder name = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 0x7E) continue;
            if (Character.isLetterOrDigit(c) || c == '_') name.append(c);
        }
        return name.toString();
    }
}

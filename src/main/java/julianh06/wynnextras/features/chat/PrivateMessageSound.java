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

@WEModule
public class PrivateMessageSound {
    private static final int PRIVATE_MESSAGE_COLOR = 0xDDCC99;

    @SubscribeEvent
    void onChatMessage(ChatEvent event) {
        if (!WynnExtrasConfig.INSTANCE.privateMessageSound) return;
        if (event.message == null) return;
        if (!isPrivateMessage(event.message)) return;

        String recipient = recipientOf(event.message.getString());
        if (!recipient.equalsIgnoreCase(MinecraftUtils.playerName())) return;

        MinecraftUtils.playSoundAmbient(
                SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.privateMessageSoundType.getSoundId())),
                WynnExtrasConfig.INSTANCE.privateMessageSoundVolume / 100,
                WynnExtrasConfig.INSTANCE.privateMessageSoundPitch / 100);
    }

    private static boolean isPrivateMessage(Text message) {
        TextColor color = firstColor(message);
        return color != null && (color.getRgb() & 0xFFFFFF) == PRIVATE_MESSAGE_COLOR;
    }

    private static TextColor firstColor(Text text) {
        TextColor own = text.getStyle().getColor();
        if (own != null) return own;
        for (Text sibling : text.getSiblings()) {
            TextColor found = firstColor(sibling);
            if (found != null) return found;
        }
        return null;
    }

    private static String recipientOf(String plain) {
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

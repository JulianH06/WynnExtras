package julianh06.wynnextras.mixin;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.features.badges.PlayerUuidCache;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mixin to add WynnExtras badges (stars) to chat messages.
 */
@Mixin(ChatHud.class)
public class ChatBadgeMixin {

    // Pattern to match player names in various chat formats
    // Matches: [Guild] Name:, [Party] Name:, Name:, and similar patterns
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "(?:\\[\\w+\\]\\s*)*(?:\\[\\w+\\]\\s+)?([A-Za-z0-9_]{3,16})(?::|\\s*\\[|\\s+)"
    );

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text modifyChatMessage(Text message) {
        if (!WynnExtrasConfig.INSTANCE.badgesEnabled) return message;
        if (!WynnExtrasConfig.INSTANCE.badgesInChat) return message;

        // Update player cache for UUID lookups
        PlayerUuidCache.updateFromPlayerList();

        String messageStr = message.getString();

        // Try to extract player name from the message
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(messageStr);
        if (matcher.find()) {
            String playerName = matcher.group(1);

            // Check if this player is a WynnExtras user
            if (PlayerUuidCache.isWynnExtrasUser(playerName)) {
                // Add the star badge after their name
                return addBadgeToMessage(message, playerName);
            }
        }

        return message;
    }

    /**
     * Adds a star badge after the player's name in the message.
     */
    private Text addBadgeToMessage(Text originalMessage, String playerName) {
        String originalStr = originalMessage.getString();
        int nameIndex = originalStr.indexOf(playerName);

        if (nameIndex == -1) return originalMessage;

        // Find the end of the player name
        int nameEnd = nameIndex + playerName.length();

        // Build a new text with the badge inserted
        MutableText result = Text.empty();

        // Add the part before and including the name
        if (nameEnd <= originalStr.length()) {
            result.append(Text.literal(originalStr.substring(0, nameEnd)));
            // Add the badge
            result.append(Text.literal(" \u2605").formatted(Formatting.DARK_GREEN));
            // Add the rest of the message
            if (nameEnd < originalStr.length()) {
                result.append(Text.literal(originalStr.substring(nameEnd)));
            }
        }

        return result;
    }
}
